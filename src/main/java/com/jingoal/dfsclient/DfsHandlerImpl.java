package com.jingoal.dfsclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import com.jingoal.dfsclient.cache.DiskCache;
import com.jingoal.dfsclient.cache.ReadCallback;
import com.jingoal.dfsclient.cache.WriteCallback;
import com.jingoal.dfsclient.metrics.PrometheusProvider;
import com.jingoal.dfsclient.transfer.Chunk;
import com.jingoal.dfsclient.transfer.ClientDescription;
import com.jingoal.dfsclient.transfer.CopyRep;
import com.jingoal.dfsclient.transfer.CopyReq;
import com.jingoal.dfsclient.transfer.DuplicateRep;
import com.jingoal.dfsclient.transfer.DuplicateReq;
import com.jingoal.dfsclient.transfer.ExistRep;
import com.jingoal.dfsclient.transfer.ExistReq;
import com.jingoal.dfsclient.transfer.FileInfo;
import com.jingoal.dfsclient.transfer.FileTransferGrpc;
import com.jingoal.dfsclient.transfer.FileTransferGrpc.FileTransferBlockingStub;
import com.jingoal.dfsclient.transfer.FileTransferGrpc.FileTransferStub;
import com.jingoal.dfsclient.transfer.GetByMd5Rep;
import com.jingoal.dfsclient.transfer.GetByMd5Req;
import com.jingoal.dfsclient.transfer.GetFileRep;
import com.jingoal.dfsclient.transfer.GetFileReq;
import com.jingoal.dfsclient.transfer.NegotiateChunkSizeRep;
import com.jingoal.dfsclient.transfer.NegotiateChunkSizeReq;
import com.jingoal.dfsclient.transfer.PutFileRep;
import com.jingoal.dfsclient.transfer.PutFileReq;
import com.jingoal.dfsclient.transfer.RemoveFileReq;
import com.jingoal.dfsclient.util.CompareUtils;
import com.jingoal.dfsclient.util.DfsConstants;
import com.jingoal.dfsclient.util.FileUtils;
import com.jingoal.dfsclient.util.StackTraceUtils;
import com.jingoal.dfsclient.util.StringUtils;
import com.jingoal.dfsclient.util.TimerUtils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;

public class DfsHandlerImpl implements DfsHandler {
  private static final Logger logger = LoggerFactory.getLogger(DfsHandlerImpl.class);

  public static final String DEADLINE_EXCEEDED = "DEADLINE_EXCEEDED";
  private static final String ERR_FILE_NOT_FOUND = "file not found";
  private static final String DUPLICATE_KEY_ERROR_INDEX = "E11000 duplicate key error index";
  private static final String INVALID_ID = "Invalid id";
  private static final Integer DUPLICATE_RETRY_INTERVAL = 30;

  private ManagedChannel channel;
  private FileTransferBlockingStub blockingStub;
  private FileTransferStub stub;
  private final InetSocketAddress addr;
  private DiskCache diskCache;
  private long chunkSize;

  public DfsHandlerImpl(final InetSocketAddress addr) throws Exception {
    this(addr, DfsConstants.getDefaultChunkSizeInBytes());
  }

  public DfsHandlerImpl(final InetSocketAddress addr, long chunkSize) throws Exception {
    this.addr = addr;
    channel = ManagedChannelBuilder.forAddress(addr.getHostName(), addr.getPort())
        .usePlaintext(true).build();
    blockingStub = FileTransferGrpc.newBlockingStub(channel);
    stub = FileTransferGrpc.newStub(channel);

    NegotiateChunkSizeRep negRep = blockingStub
        .negotiateChunkSize(NegotiateChunkSizeReq.newBuilder().setSize(chunkSize).build());
    this.chunkSize = negRep.getSize();
    logger.info("Negotiate chunk size:{}", negRep.getSize());
  }

  @Override
  public void shutdown() throws InterruptedException {
    if (channel != null) {
      channel.shutdown().awaitTermination(DfsConstants.CHANNEL_SHUTDOWN_AWAIT_SECONDS,
          TimeUnit.SECONDS);
    }
  }

  @Override
  public DFSInputStream getInputStream(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    checkParameter("getInputStream", fid, domain);
    final FileInfo fileInfo = getFileInfo(fid, domain);
    double milliseconds = calMilliSecondsByBytes(DfsConstants.METRICS_GETFILE, fileInfo.getSize());
    logger.debug("file size:{}, cal deadline:{}", fileInfo.getSize(), milliseconds);
    Iterator<GetFileRep> reps = getBlockingStub(milliseconds)
        .getFile(GetFileReq.newBuilder().setId(fid).setDomain(domain).build());

    try {
      if (reps.hasNext()) {
        reps.next();// skip fileInfo
      } else {
        return null;// will never go here.
      }
    } catch (Exception e) {
      logger.warn("{}, fid:{} domain:{}", e.getMessage(), fid, domain);
      throw new IOException("Failed to get file, fid:" + fid + " domain:" + domain);
    }

    return new DFSInputStream() {
      ByteString payload = null;
      int offset = 0; // 当前的位置，包括已经读过的和正在读的chunk的位置。
      int length = 0; // 当前的长度，当前已经读过包括在读的chunk的总长度。
      TimerUtils timer = new TimerUtils();

      @Override
      public Object getMetadata(final String key) {
        if (key.equals("_id")) {
          return fileInfo.getId();
        } else if (key.equals("filename")) {
          return fileInfo.getName();
        } else if (key.equals("length")) {
          return fileInfo.getSize();
        } else if (key.equals("domain")) {
          return fileInfo.getDomain();
        } else if (key.equals("userid")) {
          return fileInfo.getUser();
        } else if (key.equals("md5")) {
          return fileInfo.getMd5();
        } else if (key.equals("bizname")) {
          return fileInfo.getBiz();
        }
        throw new IllegalArgumentException("Not supported meta key:" + key);
      }

      @Override
      public void close() throws IOException {
        while (getNextChunk() != null) { // 释放网络上的数据，如果有的话。
        }
        doMetrics(DfsConstants.METRICS_GETFILE, fileInfo.getBiz(), fileInfo.getSize(), timer);
      }

      @Override
      public int read() throws IOException {
        byte b[] = new byte[1];
        int res = read(b);
        if (res < 0) {
          return -1;
        }
        return b[0] & 0xFF;
      }

      @Override
      public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
      }

      @Override
      public int read(final byte b[], final int off, final int len) throws IOException {
        if (payload == null || length <= offset) {
          Chunk chunk = getNextChunk();
          if (chunk == null) { // 传送完了
            return -1;
          }

          payload = chunk.getPayload();
          length = payload.size();
          offset = 0;
        }

        int r = Math.min(len, length - offset);

        try {
          payload.copyTo(b, offset, off, r);
        } catch (Exception e) {
          logger.error("{}, fid:{} domain:{}", e.getMessage(), fid, domain);
          throw new IOException("Failed to read byte, fid:" + fid + " domain:" + domain, e);
        }
        offset += r;

        return r;
      }

      private Chunk getNextChunk() throws IOException {
        try {
          if (reps.hasNext()) {
            GetFileRep rep = reps.next();
            return rep.getChunk();
          }
        } catch (Exception e) {
          doMetrics(DfsConstants.METRICS_GETFILE, e, timer, fileInfo.getBiz(), fileInfo.getSize());
          logger.error("{}, fid:{} domain:{}", e.getMessage(), fid, domain);
          throw new IOException("Failed to get next chunk, fid:" + fid + " domain:" + domain, e);
        }

        return null;
      }
    };
  }

  @Override
  public DFSOutputStream getOutputStream(final long domain, final String bizname,
      final String filename, final String userid) throws IOException, InvalidArgumentException {
    return getOutputStream(domain, bizname, filename, userid, -1);
  }

  @Override
  public DFSOutputStream getOutputStream(final long domain, final String bizname,
      final String filename, final String userid, final long size)
      throws IOException, InvalidArgumentException {
    return getOutputStream(ObjectId.get().toHexString(), domain, bizname, filename, userid, size);
  }

  @Override
  public DFSOutputStream getOutputStream(final String fid, final long domain, final String bizname,
      final String filename, final String userid, final long size)
      throws IOException, InvalidArgumentException {
    double milliseconds = calMilliSecondsByBytes(DfsConstants.METRICS_PUTFILE, size);
    logger.debug("file size:{}, cal deadline:{}", size, milliseconds);
    checkOutputStreamParameter("getOutputStream", domain, bizname, userid);

    long userId;
    try {
      userId = Long.parseLong(userid);
    } catch (NumberFormatException e) {
      logger.warn("{}, userid:{}", e.getMessage(), userid);
      throw new InvalidArgumentException("Failed to format, userid:" + userid, e);
    }

    class DFSOutputStreamImpl extends DFSOutputStream {
      private final TimerUtils timer;
      private final SettableFuture<Void> finishFuture = SettableFuture.create();
      private final Object ready = new Object();
      private final ClientCallStreamObserver<PutFileReq> requestObserver;
      private FileInfo info;
      private long length;
      private volatile boolean onError = false;

      public DFSOutputStreamImpl() {
        this.info = FileInfo.newBuilder().setId(fid).setName(filename == null ? "" : filename)
            .setDomain(domain).setBiz(bizname).setUser(userId).build();

        StreamObserver<PutFileRep> responseObserver =
            new ClientResponseObserver<PutFileReq, PutFileRep>() {

              @Override
              public void onNext(final PutFileRep rep) {
                info = rep.getFile();
                if (info != null && !StringUtils.isValidObjectId(info.getId())) {
                  onError(new Throwable("Upload fail, return invalid fid:" + info.getId()));
                }
              }

              @Override
              public void onError(final Throwable t) {
                handleError();
                finishFuture.setException(t);
              }

              @Override
              public void onCompleted() {
                finishFuture.set(null);
              }

              @Override
              public void beforeStart(ClientCallStreamObserver<PutFileReq> requestStream) {
                requestStream.setOnReadyHandler(new Runnable() {
                  @Override
                  public void run() {
                    notifySelf();
                  }
                });
              }
            };

        this.requestObserver =
            (ClientCallStreamObserver<PutFileReq>) getStub(milliseconds).putFile(responseObserver);

        this.timer = new TimerUtils();
      }

      private void notifySelf() {
        synchronized (ready) {
          ready.notifyAll();
        }
      }

      private void handleError() {
        this.onError = true;
        notifySelf();
      }

      @Override
      public void write(final byte b[], final int off, final int len) throws IOException {
        if (DfsConstants.isFlowControlEnabled()) {
          while (!onError && !requestObserver.isReady()) {
            synchronized (ready) {
              try {
                ready.wait(DfsConstants.timeoutToWaitFlowControl());
              } catch (InterruptedException e) {
              }
            }
          }
        }
        if (onError) {
          throw new IOException("The request has been cancelled.");
        }

        try {
          Chunk chunk = Chunk.newBuilder().setLength(len).setPos(0)
              .setPayload(ByteString.copyFrom(b, off, len)).build();
          PutFileReq req = PutFileReq.newBuilder().setChunk(chunk).setInfo(info).build();
          requestObserver.onNext(req);
          length += len;
        } catch (Exception e) {
          requestObserver.onError(e);
          doMetrics(DfsConstants.METRICS_PUTFILE, e, timer);
          logger.error("Failed to write, file:{} error:{}", info, e.getMessage());
          throw new IOException("Failed to write, fid:" + info.getId() + ", name:" + info.getName()
              + ", size:" + info.getSize() + ", domain:" + domain + ", biz:" + bizname, e);
        }
      }

      @Override
      public void close() throws IOException {
        if (!onError) {
          try {
            if (length == 0) {// 处理0字节文件上传
              Chunk chunk =
                  Chunk.newBuilder().setLength(0).setPos(0).setPayload(ByteString.EMPTY).build();
              PutFileReq req = PutFileReq.newBuilder().setChunk(chunk).setInfo(info).build();
              requestObserver.onNext(req);
            }
            requestObserver.onCompleted();
          } catch (Exception e) {
            requestObserver.onError(e);
            doMetrics(DfsConstants.METRICS_PUTFILE, e, timer);
            logger.error("Failed to close, file:{} error:{}", info, e.getMessage());
            throw new IOException(
                "Failed to close, fid:" + info.getId() + ", name:" + info.getName() + ", size:"
                    + info.getSize() + ", domain:" + domain + ", biz:" + bizname,
                e);
          }
        }
        try {
          finishFuture.get();
        } catch (Exception e) {
          doMetrics(DfsConstants.METRICS_PUTFILE, e, timer, bizname, length);
          logger.warn(e.getMessage(), e);
          throw new IOException(e);
        }
        doMetrics(DfsConstants.METRICS_PUTFILE, bizname, length, timer);
      }

      @Override
      public void write(final int b) throws IOException {
        byte[] bs = new byte[1];
        bs[0] = (byte) (0xff & b);
        write(bs, 0, 1);
      }

      @Override
      public String getId() {
        return info.getId();
      }

      @Override
      public long getLength() {
        return length;
      }
    }

    return new DFSOutputStreamImpl();
  }

  @Override
  public DFSDataSource getDataSource(final long domain, final String bizname, final String fid,
      final String userid) throws IOException, FileNotFoundException, InvalidArgumentException {
    if (domain <= 0) {
      throw new InvalidArgumentException("Domain must great than zero.");
    }
    if (StringUtils.isBlank(bizname)) {
      throw new InvalidArgumentException("Bizname mustn't be null.");
    }
    DFSInputStream in = getInputStream(fid, domain);
    return new DFSDataSource(in, fid);
  }

  @Override
  public boolean delete(final String fid, final long domain) throws InvalidArgumentException {
    checkParameter("delete", fid, domain);

    String methodDesc = StackTraceUtils.getStackTraceString(
        "com.jingoal.dfsclient.DfsHandlerImpl.delete", 4/* 从 delete 方法往上显示4级栈信息 */);
    RemoveFileReq req = RemoveFileReq.newBuilder().setDomain(domain).setId(fid)
        .setDesc(ClientDescription.newBuilder().setDesc(methodDesc).build()).build();
    TimerUtils timer = new TimerUtils();
    try {
      getBlockingStub().removeFile(req);
      if (diskCache != null) {
        diskCache.deleteCacheFile(fid, domain);
      }
    } catch (Exception e) {
      doMetrics(DfsConstants.METRICS_DELETE, e, timer);
      if (e.getMessage().endsWith(ERR_FILE_NOT_FOUND)) {
        return true;
      } else if (e.getMessage().indexOf(INVALID_ID) != -1) {
        throw new InvalidArgumentException("Invalid id:" + fid);
      }
      logger.warn("{}, fid:{} domain:{}", e.getMessage(), fid, domain);
      return false;
    }
    accessStat(DfsConstants.METRICS_DELETE, timer);

    return true;
  }

  @Override
  public String duplicate(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    checkParameter("duplicate", fid, domain);

    DuplicateReq req = DuplicateReq.newBuilder().setId(fid).setDomain(domain).build();
    TimerUtils timer = new TimerUtils();
    DuplicateRep rep;
    try {
      rep = getBlockingStub(DfsConstants.getDefaultReadTimeOut()).duplicate(req);
    } catch (Exception e) {
      doMetrics(DfsConstants.METRICS_DUPLICATE, e, timer);
      logger.warn("{}, fid:{} domain:{}", e.getMessage(), fid, domain);
      if (e.getMessage().endsWith(ERR_FILE_NOT_FOUND)) {
        throw new FileNotFoundException("fid:" + fid + " domain:" + domain, e);
      } else if (e.getMessage().indexOf(INVALID_ID) != -1) {
        throw new InvalidArgumentException("Invalid id:" + fid);
      } else if (e.getMessage().indexOf(DUPLICATE_KEY_ERROR_INDEX) != -1) {
        try {
          Thread.sleep(DUPLICATE_RETRY_INTERVAL);
        } catch (InterruptedException e1) {
        }
        return duplicate(fid, domain);
      }
      throw new IOException("Failed to duplicate, fid:" + fid + " domain:" + domain, e);
    }
    accessStat(DfsConstants.METRICS_DUPLICATE, timer);

    return rep.getId();
  }

  @Override
  public String copy(final long dstdomain, final String bizname, final long srcdomain,
      final String srcFid, final String userid)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    checkCopyParameter(dstdomain, bizname, srcdomain, srcFid, userid);
    if (dstdomain == srcdomain) {
      return duplicate(srcFid, srcdomain);
    }

    long uid;
    try {
      uid = Long.parseLong(userid);
    } catch (NumberFormatException e) {
      logger.warn("{}, userid:{}", e.getMessage(), userid);
      throw new InvalidArgumentException("Failed to format, userid:" + userid, e);
    }
    final FileInfo fileInfo = getFileInfo(srcFid, srcdomain);
    double milliseconds = calMilliSecondsByBytes(DfsConstants.METRICS_COPY, fileInfo.getSize());
    logger.debug("file size:{}, cal deadline:{}", fileInfo.getSize(), milliseconds);

    CopyReq req = CopyReq.newBuilder().setSrcFid(srcFid).setSrcDomain(srcdomain)
        .setDstDomain(dstdomain).setDstUid(uid).setDstBiz(bizname).build();
    TimerUtils timer = new TimerUtils();
    CopyRep rep;
    try {
      rep = getBlockingStub(milliseconds).copy(req);
    } catch (Exception e) {
      doMetrics(DfsConstants.METRICS_COPY, e, timer, bizname, fileInfo.getSize());
      logger.warn("{}, file:{} dstdomain:{}", e.getMessage(), fileInfo, dstdomain);
      if (e.getMessage().endsWith(ERR_FILE_NOT_FOUND)) {
        throw new FileNotFoundException("srcFid:" + srcFid + " srcdomain:" + srcdomain
            + " dstdomain:" + dstdomain + " userid:" + userid + " bizname:" + bizname, e);
      } else if (e.getMessage().indexOf(INVALID_ID) != -1) {
        throw new InvalidArgumentException("Invalid id:" + srcFid);
      } else if (e.getMessage().indexOf(DUPLICATE_KEY_ERROR_INDEX) != -1) {
        try {
          Thread.sleep(DUPLICATE_RETRY_INTERVAL);
        } catch (InterruptedException e1) {
        }
        return copy(dstdomain, bizname, srcdomain, srcFid, userid);
      }
      throw new IOException(
          "Failed to copy, srcFid:" + srcFid + " srcdomain:" + srcdomain + " dstdomain:" + dstdomain
              + " userid:" + userid + " bizname:" + bizname + " filesize:" + fileInfo.getSize(),
          e);
    }
    doMetrics(DfsConstants.METRICS_COPY, bizname, fileInfo.getSize(), timer);

    return rep.getFid();
  }

  private void checkCopyParameter(final long dstdomain, final String bizname, final long srcdomain,
      final String srcFid, final String userid) throws InvalidArgumentException {
    if (dstdomain <= 0 || srcdomain <= 0) {
      throw new InvalidArgumentException("Failed to copy , domain must great than zero.");
    }
    if (StringUtils.isBlank(bizname)) {
      throw new InvalidArgumentException("Failed to copy, bizname mustn't be null.");
    }
    if (StringUtils.isBlank(srcFid)) {
      throw new InvalidArgumentException("Failed to copy, srcFid mustn't be null.");
    }
    if (!StringUtils.isValidObjectId(srcFid)) {
      throw new InvalidArgumentException("Failed to copy, invalid fid: " + srcFid);
    }
    if (StringUtils.isBlank(userid)) {
      throw new InvalidArgumentException("Failed to copy, userid mustn't be null.");
    }
  }

  @Override
  public String getByMd5(final long domain, final String md5, final long size)
      throws IOException, InvalidArgumentException {
    checkByDomainMd5AndSize("getByMd5", domain, md5, size);

    GetByMd5Req req = GetByMd5Req.newBuilder().setDomain(domain).setMd5(md5).setSize(size).build();
    TimerUtils timer = new TimerUtils();
    GetByMd5Rep rep;
    try {
      rep = getBlockingStub(DfsConstants.getDefaultReadTimeOut()).getByMd5(req);
    } catch (Exception e) {
      doMetrics(DfsConstants.METRICS_GETBYMD5, e, timer);
      if (e.getMessage().endsWith(ERR_FILE_NOT_FOUND)) {
        return null;
      } else if (e.getMessage().indexOf(DUPLICATE_KEY_ERROR_INDEX) != -1) {
        try {
          Thread.sleep(DUPLICATE_RETRY_INTERVAL);
        } catch (InterruptedException e1) {
        }
        return getByMd5(domain, md5, size);
      }
      logger.warn("{}, domain:{} md5:{} size:{}", e.getMessage(), domain, md5, size);
      throw new IOException(
          "Failed to getByMd5, domain:" + domain + " md5:" + md5 + " size:" + size, e);
    }
    accessStat(DfsConstants.METRICS_GETBYMD5, timer);

    return rep.getFid();
  }

  @Override
  public boolean existByMd5(final long domain, final String md5, final long size)
      throws IOException, InvalidArgumentException {
    checkByDomainMd5AndSize("existByMd5", domain, md5, size);

    GetByMd5Req req = GetByMd5Req.newBuilder().setDomain(domain).setMd5(md5).setSize(size).build();
    TimerUtils timer = new TimerUtils();
    ExistRep rep;
    try {
      rep = getBlockingStub(DfsConstants.getDefaultReadTimeOut()).existByMd5(req);
    } catch (Exception e) {
      doMetrics(DfsConstants.METRICS_EXISTBYMD5, e, timer);
      if (e.getMessage().endsWith(ERR_FILE_NOT_FOUND)) {
        return false;
      }
      logger.warn("{},  domain:{} md5:{} size:{}", e.getMessage(), domain, md5, size);
      throw new IOException(
          "Failed to existByMd5, domain:" + domain + " md5:" + md5 + " size:" + size, e);
    }
    accessStat(DfsConstants.METRICS_EXISTBYMD5, timer);

    return rep.getResult();
  }

  private void checkByDomainMd5AndSize(final String method, final long domain, final String md5,
      final long size) throws InvalidArgumentException {
    if (domain <= 0) {
      throw new InvalidArgumentException("Failed to " + method + " , domain must great than zero.");
    }
    if (StringUtils.isBlank(md5)) {
      throw new InvalidArgumentException("Failed to " + method + ", md5 mustn't be null.");
    }
    if (size < 0) {
      throw new InvalidArgumentException(
          "Failed to " + method + ", size must be great than zero, size: " + size);
    }
  }

  @Override
  public boolean exist(final String fid, final long domain)
      throws IOException, InvalidArgumentException {
    checkParameter("exist", fid, domain);

    ExistReq req = ExistReq.newBuilder().setId(fid).setDomain(domain).build();
    TimerUtils timer = new TimerUtils();
    ExistRep rep;
    try {
      rep = getBlockingStub(DfsConstants.getDefaultReadTimeOut()).exist(req);
    } catch (Exception e) {
      doMetrics(DfsConstants.METRICS_EXIST, e, timer);
      if (e.getMessage().endsWith(ERR_FILE_NOT_FOUND)) {
        return false;
      } else if (e.getMessage().indexOf(INVALID_ID) != -1) {
        throw new InvalidArgumentException("Invalid id:" + fid);
      }
      logger.warn("{}, fid:{} domain:{}", e.getMessage(), fid, domain);
      throw new IOException("Failed to exist, fid:" + fid + " domain:" + domain, e);
    }
    accessStat(DfsConstants.METRICS_EXIST, timer);

    return rep.getResult();
  }

  @Override
  public File getFile(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    if (diskCache == null) {
      throw new RuntimeException("Please set disk cache first.");
    }
    checkParameter("getFile", fid, domain);
    boolean isExist = exist(fid, domain);
    if (!isExist) {
      throw new FileNotFoundException("fid: " + fid + ", domain: " + domain);
    }
    return diskCache.getFile(fid, new ReadCallback() {

      @Override
      public void writeTo(final OutputStream outputStream)
          throws IOException, FileNotFoundException, InvalidArgumentException {
        InputStream inputStream = getInputStream(fid, domain);
        FileUtils.copy(inputStream, outputStream);
      }

      @Override
      public long getDomain() {
        return domain;
      }
    });
  }

  @Override
  public String putFile(final long domain, final String bizname, final String filename,
      final File file, final String userid) throws IOException, InvalidArgumentException {
    if (diskCache == null) {
      throw new RuntimeException("Please set disk cache first.");
    }
    checkPutFileParameter("putFile", domain, bizname, file, userid);

    final String fname = (filename != null) ? filename : file.getName();
    return diskCache.putFile(file, new WriteCallback() {

      @Override
      public String readFrom(final InputStream inputStream)
          throws IOException, InvalidArgumentException {
        DFSOutputStream outputStream =
            DfsHandlerImpl.this.getOutputStream(domain, bizname, fname, userid, file.length());
        FileUtils.copyLarge(inputStream, outputStream, (int) chunkSize);
        return outputStream.getId();
      }

      @Override
      public long getDomain() {
        return domain;
      }
    });
  }

  @Override
  public String putFileWithoutMove(final long domain, final String bizname, final String filename,
      final File file, final String userid) throws IOException, InvalidArgumentException {
    if (diskCache == null) {
      throw new RuntimeException("Please set disk cache first.");
    }
    checkPutFileParameter("putFile", domain, bizname, file, userid);

    final String fname = (filename != null) ? filename : file.getName();
    return diskCache.putFileWithoutMove(file, new WriteCallback() {

      @Override
      public String readFrom(final InputStream inputStream)
          throws IOException, InvalidArgumentException {
        DFSOutputStream outputStream =
            DfsHandlerImpl.this.getOutputStream(domain, bizname, fname, userid, file.length());
        FileUtils.copyLarge(inputStream, outputStream, (int) chunkSize);
        return outputStream.getId();
      }

      @Override
      public long getDomain() {
        return domain;
      }
    });
  }

  @Override
  public String putFileNotIntoCache(final long domain, final String bizname, final String filename,
      final File file, final String userid) throws IOException, InvalidArgumentException {
    checkPutFileParameter("putFileNotIntoCache", domain, bizname, file, userid);

    final String fname = (filename != null) ? filename : file.getName();
    FileInputStream inputStream = new FileInputStream(file);
    DFSOutputStream outputStream = getOutputStream(domain, bizname, fname, userid, file.length());
    FileUtils.copyLarge(inputStream, outputStream, (int) chunkSize);

    return outputStream.getId();
  }

  private void checkPutFileParameter(final String method, final long domain, final String bizname,
      final File file, final String userid) throws InvalidArgumentException {
    if (file == null) {
      throw new InvalidArgumentException("Failed to " + method + ", file mustn't be null.");
    }
    if (!(file.exists() && file.isFile())) {
      throw new InvalidArgumentException("Failed to " + method + ", must input valid file.");
    }
    checkOutputStreamParameter(method, domain, bizname, userid);
  }

  private void checkOutputStreamParameter(final String method, final long domain,
      final String bizname, final String userid) throws InvalidArgumentException {
    if (domain <= 0) {
      throw new InvalidArgumentException("Failed to " + method + " , domain must great than zero.");
    }
    if (StringUtils.isBlank(bizname)) {
      throw new InvalidArgumentException("Failed to " + method + " , bizname mustn't be null.");
    }
    if (StringUtils.isBlank(userid)) {
      throw new InvalidArgumentException("Failed to " + method + ", userid mustn't be null.");
    }
  }

  private void checkParameter(final String method, final String fid, final long domain)
      throws InvalidArgumentException {
    if (domain <= 0) {
      throw new InvalidArgumentException("Failed to " + method + " , domain must great than zero.");
    }
    if (StringUtils.isBlank(fid)) {
      throw new InvalidArgumentException("Failed to " + method + " , fid mustn't be null.");
    }
    if (!StringUtils.isValidObjectId(fid)) {
      throw new InvalidArgumentException("Failed to " + method + " , invalid fid: " + fid);
    }
  }

  /**
   * When param greater than 0, will return Stub with deadline.
   *
   * @param milliseconds
   * @return
   */
  private FileTransferBlockingStub getBlockingStub() {
    return getBlockingStub(0);
  }

  private FileTransferBlockingStub getBlockingStub(final double milliseconds) {
    verifyChannelState();
    long duration = Double.valueOf(milliseconds).longValue();
    if (DfsConstants.isDeadlineEnable() && duration > 0) {
      return blockingStub.withDeadlineAfter(duration, TimeUnit.MILLISECONDS);
    }
    return blockingStub;
  }

  /**
   * When param greater than 0, will return Stub with deadline.
   *
   * @param milliseconds
   * @return
   */
  private FileTransferStub getStub(final double milliseconds) {
    verifyChannelState();
    long duration = Double.valueOf(milliseconds).longValue();
    if (DfsConstants.isDeadlineEnable() && duration > 0) {
      return stub.withDeadlineAfter(duration, TimeUnit.MILLISECONDS);
    }
    return stub;
  }

  private void verifyChannelState() {
    synchronized (this) {
      if (channel.isShutdown() || channel.isTerminated()) {
        channel = ManagedChannelBuilder.forAddress(addr.getHostName(), addr.getPort())
            .usePlaintext(true).build();
        blockingStub = FileTransferGrpc.newBlockingStub(channel);
        stub = FileTransferGrpc.newStub(channel);
      }
    }
  }

  /**
   * When fail,metrics the counter, and when exceeded, need to metrics the counter and elapse time.
   *
   * @param e
   * @param createTime
   */
  private void doMetrics(final String name, final Exception e, TimerUtils timer) {
    if (StringUtils.isNotBlank(e.getMessage()) && e.getMessage().indexOf(DEADLINE_EXCEEDED) != -1) {
      PrometheusProvider.timeoutStat(name, timer);
    } else {
      PrometheusProvider.failCounter.labels(name).inc();
    }
  }

  private void doMetrics(final String name, final Exception e, TimerUtils timer, String bizname,
      long fileSize) {
    if (StringUtils.isNotBlank(e.getMessage()) && e.getMessage().indexOf(DEADLINE_EXCEEDED) != -1) {
      PrometheusProvider.timeoutStat(name, timer);
      PrometheusProvider.filesizeGauge.labels(name, bizname).set(fileSize);
      // When occurring deadline, reduce the KBps value, adaptive the transfer speed.
      PrometheusProvider.kbpsGauge.labels(name)
          .set(getKBps(fileSize, timer.elapsedMillis() * DfsConstants.getDeadlineAdjustFactor()));
    } else {
      PrometheusProvider.failCounter.labels(name).inc();
    }
  }

  /**
   * Metrics the file size, elapsed and kbps.
   *
   * @param fileSize
   * @param createTime
   */
  private void doMetrics(String name, String bizname, long fileSize, TimerUtils timer) {
    PrometheusProvider.filesizeGauge.labels(name, bizname).set(fileSize);
    accessStat(name, timer);
    PrometheusProvider.kbpsGauge.labels(name).set(getKBps(fileSize, timer.elapsedMillis()));
  }

  private void accessStat(String name, TimerUtils timer) {
    PrometheusProvider.accessTime.labels(name).set(timer.elapsedMillis());
  }

  private double getKBps(long bytes, double milliseconds) {
    // To prevent the divisor is zero.
    if (milliseconds == 0) {
      milliseconds = 1;
    }
    return new BigDecimal((bytes * 1000) / (milliseconds * DfsConstants.KB))
        .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
  }

  private double defaultKBps = 512;// transfer rate in KByte/s.

  /**
   * When param less than 0,return -1; Otherwise will be greater than 60 seconds.
   *
   * @param bytes
   * @return
   */
  private double calMilliSecondsByBytes(final String name, final long bytes) {
    if (bytes < 0) {
      return -1;
    }
    double quantile = PrometheusProvider.kbpsGauge.labels(name).get();
    if (quantile == 0) {
      quantile = defaultKBps;
    }
    BigDecimal milliseconds = new BigDecimal((bytes * 1000) / (quantile * DfsConstants.KB));
    milliseconds = milliseconds.multiply(new BigDecimal(DfsConstants.getDeadlineAdjustFactor()));

    double result = milliseconds.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    if (CompareUtils.compareTo(result, DfsConstants.getMinStreamTimeOut()) < 0) {
      result = DfsConstants.getMinStreamTimeOut();
    }
    return result;
  }

  @Override
  public FileInfo getFileInfo(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    FileInfo fileInfo = null;
    try {
      PutFileRep rep = getBlockingStub(DfsConstants.getDefaultReadTimeOut())
          .stat(GetFileReq.newBuilder().setId(fid).setDomain(domain).build());
      fileInfo = rep.getFile();
    } catch (Exception e) {
      logger.warn("{}, fid:{} domain:{}", e.getMessage(), fid, domain);
      if (e.getMessage().endsWith(ERR_FILE_NOT_FOUND)) {
        throw new FileNotFoundException("fid:" + fid + " domain:" + domain, e);
      } else if (e.getMessage().indexOf(INVALID_ID) != -1) {
        throw new InvalidArgumentException("Invalid id:" + fid);
      }
      throw new IOException("Failed to get file, fid:" + fid + " domain:" + domain, e);
    }
    if (fileInfo == null) {
      throw new FileNotFoundException("fid:" + fid + " domain:" + domain);
    }
    return fileInfo;
  }

  @Override
  public void setDiskCache(final DiskCache diskCache) {
    this.diskCache = diskCache;
  }
}
