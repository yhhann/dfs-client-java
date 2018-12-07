package com.jingoal.dfsclient.test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.DFSClientImpl;
import com.jingoal.dfsclient.DFSInputStream;
import com.jingoal.dfsclient.DFSOutputStream;
import com.jingoal.dfsclient.DFSShardClient;
import com.jingoal.dfsclient.cache.DiskCache;
import com.jingoal.dfsclient.cache.LocalDiskCache;
import com.jingoal.dfsclient.util.FileUtils;
import com.jingoal.dfsclient.util.IoMd5Utils;

public class Stress {
  private static final Logger logger = LoggerFactory.getLogger(Stress.class);

  private final DFSShardClient client;
  private final byte[] byteArray;
  private final long domain;
  private boolean running = true;
  // You can create the file '/tmp/1g' with the following command on linux box:
  // dd if=/dev/zero of=/tmp/1g bs=1048576 count=1024
  private String testFn = System.getProperty("dfs.testfn", "/tmp/1g");
  private int fileCount = 1;
  private boolean readable = false;
  private CountDownLatch latch;

  public Stress() {
    String dfsSeeds = System.getProperty("dfs.seeds", "dfslb://127.0.0.1:10000");
    String cacheDir = System.getProperty("dfs.cache", "/tmp");
    String clientId = System.getProperty("dfs.clientid", UUID.randomUUID().toString());
    String lengthStr = System.getProperty("dfs.length", "4096");
    String domainStr = System.getProperty("dsf.domain", "1");
    this.readable = "true".equals(System.getProperty("dfs.readable", "false"));
    this.fileCount = Integer.parseInt(System.getProperty("dfs.file.count", "1"));

    int length = Integer.valueOf(lengthStr);
    this.byteArray = new byte[length];
    this.domain = Long.valueOf(domainStr);

    this.latch = new CountDownLatch(this.fileCount);

    DiskCache cache = new LocalDiskCache(cacheDir, 10 * 1000L);
    client = new DFSClientImpl(dfsSeeds, clientId, cache);
  }

  private String WriteFile(long domain) {
    String fn = System.currentTimeMillis() + "";
    try {
      DFSOutputStream os = this.client.getOutputStream(domain, "java-press-test", fn, "10010");
      FileUtils.copy(new ByteArrayInputStream(byteArray), os); // close
      return os.getId() + "," + fn;
    } catch (Exception e) {
      logger.warn("WriteFile {}\n", fn, e.getMessage());
    }

    return "";
  }

  private String CopyFile(long domain) {
    String fn = System.currentTimeMillis() + "";
    try {
      InputStream is = new FileInputStream(testFn);
      DFSOutputStream os = this.client.getOutputStream(domain, "java-press-test", fn, "10010");
      FileUtils.copy(is, os); // close
      return os.getId() + "," + fn;
    } catch (Exception e) {
      logger.warn("WriteFile {}\n", fn, e.getMessage());
    }

    return "";
  }

  private boolean ReadFile(String fid, long domain) {
    try {
      MessageDigest digest = IoMd5Utils.getDigest();
      DFSInputStream is = this.client.getInputStream(fid, domain);

      int n = 0;
      byte[] b = new byte[4096];
      while (-1 != (n = is.read(b))) {
        digest.update(b);
      }
      String origMd5 = (String) is.getMetadata("md5");
      String md5 = IoMd5Utils.toHex(digest.digest());

      return md5.equals(origMd5);
    } catch (Exception e) {
      logger.warn("ReadFile {}\n", fid, e.getMessage());
    }

    return false;
  }

  Runnable r = new Runnable() {

    @Override
    public void run() {
          String fidStr = "";
          fidStr = CopyFile(domain);
          if (fidStr != null && !fidStr.trim().equals("")) {
            String[] tmp = fidStr.split(",");
            String fid = tmp[0];
            String fn = "nothing";
            if (tmp.length > 1) {
              fn = tmp[1];
            }
            logger.info("Write file {}.", fid);
            if(readable && !"".equals(fid)) {
              logger.info("Read file {} {}.", fn, ReadFile(fid, domain));
            }
            latch.countDown();
          }
      }
  };



  public static void main(String[] args) {
    Stress stress = new Stress();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        stress.running = false;
      }
    });

    long start = System.currentTimeMillis();
    ExecutorService fixedThreadPool = Executors.newFixedThreadPool(stress.fileCount);

    for(int i = 0; i < stress.fileCount; i++) {
      fixedThreadPool.execute(stress.r);
    }

    try {
      stress.latch.await();
    } catch (InterruptedException e) {
    }

    fixedThreadPool.shutdownNow();

    logger.info("Save {} files, elapse {}", stress.fileCount, System.currentTimeMillis() - start);

    System.exit(0);
  }
}