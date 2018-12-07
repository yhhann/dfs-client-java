package com.jingoal.dfsclient.test;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jingoal.dfsclient.DFSClientImplV20;
import com.jingoal.dfsclient.DFSShardClientV20;
import com.jingoal.dfsclient.cache.DiskCache;
import com.jingoal.dfsclient.cache.LocalDiskCache;
import com.jingoal.dfsclient.util.ThreadPoolUtils;

public class DfsLoader {
  private static final Logger logger = LoggerFactory.getLogger(DfsLoader.class);

  private DFSShardClientV20 client;
  private ExecutorService pool;
  private GetFileDeque deque;
  private PutDirProcessor putDirProcessor;
  private PutFileProcessor putFileProcessor;

  private String seeds = "dfslb://192.168.52.238:10000,192.168.52.238:20000/";
  private String pingDir = "/tmp/data1";
  private String pangDir = "/tmp/data2";
  private int poolsize = 5;// 并发处理线程数

  private long domain = 1;// 公司ID
  private String localDir = System.getProperty("java.io.tmpdir");// 文件目录
  private int count = 100;// 生成文件数量
  private int length = 4;// 生成文件大小限制
  private int wBatch = 10;// 写入文件批次个数
  private int rBatch = 20;// 读取文件批次个数

  public void execute() throws Exception {
    try {
      initClient();
      pool = Executors.newFixedThreadPool(poolsize,
          new ThreadFactoryBuilder().setDaemon(true).setNameFormat("dfs-%d").build());
      deque = new GetFileDeque(rBatch);
      deque.setClient(client);
      deque.setThreadPool(pool);
      deque.setDomain(domain);

      initDirProcessor();
      initFileProcessor();
      dispatch();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    } finally {
      if (deque != null) {
        deque.clean();
      }
      if (pool != null) {
        logger.info("Wait metrics report complete.");
        ThreadPoolUtils.sleep(60 * 1000);
        logger.info("Shutdown thread pool.");
        ThreadPoolUtils.shutdownAndAwaitTermination(pool);
      }
    }
  }

  private void dispatch() throws Exception {
    if (isDirHandle()) {
      File fileDir = new File(localDir);
      File[] fileArray = fileDir.listFiles(new FileFilter() {

        @Override
        public boolean accept(final File file) {
          if (file.isFile()) {
            return true;
          }
          return false;
        }
      });
      putDirProcessor.process(fileArray);
      count = fileArray.length;
    } else {
      putFileProcessor.process();
    }
    logger.info("Total files for upload {}", count);
  }

  private void initClient() {
    DiskCache pingCache = new LocalDiskCache(pingDir, 1000 * 10);
    DiskCache pangCache = new LocalDiskCache(pangDir, 1000 * 10);
    client = new DFSClientImplV20(seeds, "dfsclient", pingCache, pangCache);
  }

  private void initDirProcessor() {
    putDirProcessor = new PutDirProcessor();
    putDirProcessor.setPool(pool);
    putDirProcessor.setClient(client);
    putDirProcessor.setDeque(deque);
    putDirProcessor.setDomain(domain);
    putDirProcessor.setBatch(wBatch);
  }

  private void initFileProcessor() {
    putFileProcessor = new PutFileProcessor();
    putFileProcessor.setPool(pool);
    putFileProcessor.setClient(client);
    putFileProcessor.setDeque(deque);
    putFileProcessor.setDomain(domain);
    putFileProcessor.setCount(count);
    putFileProcessor.setLength(length);
    putFileProcessor.setBatch(wBatch);
  }

  public void setSeeds(String seeds) {
    this.seeds = seeds;
  }

  public void setPingDir(final String pingDir) {
    this.pingDir = pingDir;
  }

  public void setPangDir(final String pangDir) {
    this.pangDir = pangDir;
  }

  public void setPoolsize(final int poolsize) {
    this.poolsize = poolsize;
  }

  public void setDomain(final long domain) {
    this.domain = domain;
  }

  public void setLocalDir(final String localDir) {
    this.localDir = localDir;
  }

  public void setCount(final int count) {
    this.count = count;
  }

  public void setLength(final int length) {
    this.length = length;
  }

  public void setwBatch(final int wBatch) {
    this.wBatch = wBatch;
  }

  public void setrBatch(final int rBatch) {
    this.rBatch = rBatch;
  }

  public static boolean isDirHandle() {
    return "true".equals(System.getProperty("dfs.dir.handle", "true"));
  }

  public static void main(final String[] args) throws Exception {
    for (int i = 0; i < args.length; i++) {
      System.out.println("arg[" + i + "]=" + args[i]);
    }

    if (args.length < 10) {
      System.out.println(
          "Usage: java DfsLoader seeds pingDir pangDir poolsize domain localDir count length wBatch rBatch");
      // dfslb://192.168.52.238:10000,192.168.52.238:20000 /tmp/data1 /tmp/data2 5 1 /tmp 5 100 1 1
      // dfs.dir.handle=true
      return;
    }
    System.setProperty("dfs.dir.handle", "false");

    DfsLoader loader = new DfsLoader();
    loader.setSeeds(args[0]);
    loader.setPingDir(args[1]);
    loader.setPangDir(args[2]);
    loader.setPoolsize(Integer.parseInt(args[3]));
    loader.setDomain(Long.parseLong(args[4]));
    loader.setLocalDir(args[5]);
    loader.setCount(Integer.parseInt(args[6]));
    loader.setLength(Integer.parseInt(args[7]));
    loader.setwBatch(Integer.parseInt(args[8]));
    loader.setrBatch(Integer.parseInt(args[9]));
    loader.execute();
  }
}
