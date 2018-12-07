package com.jingoal.dfsclient.performance.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.DFSInputStream;
import com.jingoal.dfsclient.DFSOutputStream;
import com.jingoal.dfsclient.DFSShardClientV20;
import com.jingoal.dfsclient.InvalidArgumentException;
import com.jingoal.dfsclient.util.DfsConstants;
import com.jingoal.dfsclient.util.IoMd5Utils;
import com.jingoal.dfsclient.util.TimerUtils;

public class BizTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(BizTask.class);

  private int fileLength;
  private long domain;
  private String bizName;
  private String userId;
  private long dstDomain;
  private String srcFid;

  public BizTask(int fileLength, long domain, String bizName, String userId, long dstDomain) {
    this.fileLength = fileLength;
    this.domain = domain;
    this.bizName = bizName;
    this.userId = userId;
    this.dstDomain = dstDomain;
  }

  @Override
  public void run() {
    DFSShardClientV20 handler = DFSClientPerfTest.getClient();
    TimerUtils timer0 = getTimerUtils();
    MessageDigest msgDigest = IoMd5Utils.getDigest();
    try {
      ByteArrayInputStream byteInputStream = InputStreamGenerator.getFileStream(fileLength);
      DFSOutputStream outputStream = handler.getOutputStream(domain, bizName, null, userId);
      myPutFile(byteInputStream, outputStream, msgDigest);
      srcFid = outputStream.getId();
      String md5 = IoMd5Utils.toHex(msgDigest.digest());

      PrometheusMetrics.kbpsPutFile.set(getKBps(fileLength, timer0.elapsedMillis()));
      PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_PUTFILE).inc();
      logger.info("putFile : srcFid is {} ,file length is {} ", srcFid, fileLength);
      // invoked methods
      List<Integer> invokedMethods = new ArrayList<Integer>();
      boolean isCompletion = false;
      Object duplicateResult = null;
      Object copyResult = null;
      Object getByMD5Result = null;
      while (!isCompletion) {
        MethodRandomSelector randomMethod = MethodRandomSelector.getMethod();
        if (invokedMethods.contains(randomMethod.getIndex())) {
          continue;
        }

        Class clazz = handler.getClass();
        Method[] methods = clazz.getMethods();
        Method targetMethod = null;

        for (Method m : methods) {
          if (m.getName().equals(randomMethod.getName())) {
            targetMethod = m;
            break;
          }
        }

        if (null != targetMethod) {
          TimerUtils timer = getTimerUtils();
          int methodIndex = randomMethod.getIndex();
          switch (methodIndex) {
            case 1:// duplicate
              duplicateResult = targetMethod.invoke(handler, new Object[] {srcFid, domain});
              PrometheusMetrics.duplicateGauge.set(timer.elapsedMillis());
              PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_DUPLICATE).inc();
              logger.info("duplicate : {}", duplicateResult);
              break;
            case 2:// copy
              copyResult = targetMethod.invoke(handler,
                  new Object[] {dstDomain, bizName, domain, srcFid, userId});
              PrometheusMetrics.copyGauge.set(timer.elapsedMillis());
              PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_COPY).inc();
              logger.info("copy : {}", copyResult);
              break;
            case 3:// existByMd5
              Object result3 = targetMethod.invoke(handler, new Object[] {domain, md5, fileLength});
              PrometheusMetrics.existByMd5Gauge.set(timer.elapsedMillis());
              PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_EXISTBYMD5).inc();
              logger.info("existByMd5 : {}", result3);
              break;
            case 4:// getByMd5
              getByMD5Result = targetMethod.invoke(handler, new Object[] {domain, md5, fileLength});
              PrometheusMetrics.getByMd5Gauge.set(timer.elapsedMillis());
              PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_GETBYMD5).inc();
              logger.info("getByMd5 : {}", getByMD5Result);
              break;
            case 5:// exist
              Object result5 = targetMethod.invoke(handler, new Object[] {srcFid, domain});
              PrometheusMetrics.existGauge.set(timer.elapsedMillis());
              PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_EXIST).inc();
              logger.info("exist : {}", result5);
              break;
            case 6:// getFileInfo
              Object result6 = targetMethod.invoke(handler, new Object[] {srcFid, domain});
              PrometheusMetrics.getFileInfoGauge.set(timer.elapsedMillis());
              PrometheusMetrics.invokedCounter.labels(randomMethod.getName()).inc();
              logger.info("getFileInfo : {}", result6);
              break;
            case 7:// getFile
              try (InputStream inputStream = handler.getInputStream(srcFid, domain);) {
                logger.info("getFile : {}", ((DFSInputStream)inputStream).getMetadata("length"));
                String readMd5 = IoMd5Utils.md5(inputStream).getMd5();
                PrometheusMetrics.kbpsGetFile.set(getKBps(fileLength, timer.elapsedMillis()));
                PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_GETFILE).inc();
                if (!md5.equals(readMd5)) {
                  logger.info(
                      "'GETFILE' method occur exception,the value of getFile '{}' not equals the original md5 value {}.",
                      readMd5, md5);
                }
              }
              break;
            default:
              throw new Exception("Method don't find!");
          }
          invokedMethods.add(methodIndex);
        }

        if (invokedMethods.size() == 7) {
          logger.info("Random method invoked complete");
          break;
        }
      }

      deleteFile(handler, String.valueOf(duplicateResult), domain);
      deleteFile(handler, String.valueOf(copyResult), dstDomain);
      deleteFile(handler, String.valueOf(getByMD5Result), domain);
    } catch (Exception e) {
      logger.error("An error occurred during processing of the task.", e);
    }

    TimerUtils timer = getTimerUtils();
    // delete
    boolean result = deleteFile(handler, srcFid, domain);
    PrometheusMetrics.deleteGauge.set(timer.elapsedMillis());
    logger.info("delete : " + result);
  }

  private boolean deleteFile(DFSShardClientV20 handler,String fid,long domain) {
    try {
      handler.delete(srcFid, domain);
    } catch (InvalidArgumentException e) {
      logger.error("Delete file error, fileId is {} , domain is {}.", fid, domain, e);
      return false;
    }
    PrometheusMetrics.invokedCounter.labels(DfsConstants.METRICS_DELETE).inc();
    return true;
  }

  public static long myPutFile(final InputStream input, final OutputStream output,
      final MessageDigest messageDigester) throws IOException {
    long count = 0;
    int n = 0;
    try {
      byte[] buffer = new byte[(int) DfsConstants.getDefaultChunkSizeInBytes()];
      while (-1 != (n = input.read(buffer))) {
        output.write(buffer, 0, n);
        count += n;
        messageDigester.update(buffer, 0, n);
      }
    } finally {
      try {
        input.close();
      } finally {
        output.close();
      }
    }
    return count;
  }

  private double getKBps(long bytes, double milliseconds) {
    // To prevent the divisor is zero.
    if (milliseconds == 0) {
      milliseconds = 1;
    }
    return new BigDecimal((bytes * 1000) / (milliseconds * DfsConstants.KB))
        .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
  }

  private TimerUtils getTimerUtils() {
    return new TimerUtils();
  }
}
