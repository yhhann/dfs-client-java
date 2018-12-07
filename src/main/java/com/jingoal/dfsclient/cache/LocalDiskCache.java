package com.jingoal.dfsclient.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jingoal.dfsclient.FileNotFoundException;
import com.jingoal.dfsclient.InvalidArgumentException;
import com.jingoal.dfsclient.metrics.PrometheusProvider;
import com.jingoal.dfsclient.util.DfsConstants;
import com.jingoal.dfsclient.util.FileUtils;

/**
 * Cache implementation based on file directory.
 *
 */
public class LocalDiskCache implements DiskCache {

  private static final Integer TRUNCATE_SIZE = 2;
  private String basedir;
  private long listenInterval = 1000 * 10;
  private SwitchCallback sc;
  private static final Logger logger = LoggerFactory.getLogger(LocalDiskCache.class);

  public LocalDiskCache(final String basedir, final long listenInterval) {
    this.basedir = basedir;
    this.listenInterval = listenInterval;
  }

  /**
   * This method will be called by the primary cache. Monitor the primary cache directory, when
   * there is a failure, will switch to secondary cache.
   *
   * @param sc
   */
  public void setSwitchCallback(final SwitchCallback sc) {
    this.sc = sc;
    if (this.sc != null) {
      startCacheListener(listenInterval);
    }
  }

  /**
   * Get the cache unique ID.
   *
   * @return
   */
  public String getUuid() {
    return basedir;
  }

  @Override
  public File getFile(final String fid, final ReadCallback callback)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    PrometheusProvider.cacheTotalCounter.inc();
    long domain = callback.getDomain();

    File f = FileUtils.getAndEnsureFile4Write(basedir, FileUtils.PATH_LEVEL4, domain, fid,
        TRUNCATE_SIZE);
    if (f.exists()) {
      PrometheusProvider.cacheHitCounter.inc();
      return f;
    }
    callback.writeTo(new FileOutputStream(f));
    return f;
  }

  @Override
  public String putFile(final File file, final WriteCallback callback)
      throws IOException, InvalidArgumentException {
    String fid = callback.readFrom(new FileInputStream(file));
    long domain = callback.getDomain();

    File cachedFile = FileUtils.getAndEnsureFile4Write(basedir, FileUtils.PATH_LEVEL4, domain, fid,
        TRUNCATE_SIZE);
    if (!file.renameTo(cachedFile)) {
      FileUtils.copy(new FileInputStream(file), new FileOutputStream(cachedFile));
    }
    return fid;
  }

  @Override
  public String putFileWithoutMove(final File file, final WriteCallback callback)
      throws IOException, InvalidArgumentException {
    String fid = callback.readFrom(new FileInputStream(file));
    long domain = callback.getDomain();

    File cachedFile = FileUtils.getAndEnsureFile4Write(basedir, FileUtils.PATH_LEVEL4, domain, fid,
        TRUNCATE_SIZE);

    try {
      FileUtils.copy(new FileInputStream(file), new FileOutputStream(cachedFile));
    } catch (IOException e) {
      logger.warn("Faild to write cache,will try again. {}, fid:{} domain:{} cacheFilePath:{}",
          e.getMessage(), fid, domain,cachedFile.getPath());
      PrometheusProvider.writeCacheFailedCounter.inc();
      tryWriteCache(file, fid, domain, cachedFile);
    }
    return fid;
  }

  protected void tryWriteCache(final File file, String fid, long domain, File cachedFile)
      throws IOException{
    String srcPath = cachedFile.getPath().substring(0,
        cachedFile.getPath().indexOf(File.separator+String.valueOf(domain))+
        String.valueOf(domain).length()+1);
    File destFile = new File(srcPath+"_");
    File srcFile = new File(srcPath);
    srcFile.renameTo(destFile);

    try {
      File newCachedFile = FileUtils.getAndEnsureFile4Write(basedir, FileUtils.PATH_LEVEL4, domain, fid,
          TRUNCATE_SIZE);
      FileUtils.copy(new FileInputStream(file), new FileOutputStream(newCachedFile));
    } catch (IOException e) {
      PrometheusProvider.writeCacheFailedCounter.inc();
      logger.error("Faild to write cache, throw error. {}, fid:{} domain:{} cacheFilePath:{}",
          e.getMessage(), fid, domain,cachedFile.getPath());
      throw e;
    }
  }

  @Override
  public boolean deleteCacheFile(final String fid, final long domain) {
    File cachedFile;
    try {
      cachedFile = FileUtils.getFile4ReadAndDelete(basedir, FileUtils.PATH_LEVEL4, domain, fid,
          TRUNCATE_SIZE);
      cachedFile.delete();
      return true;
    } catch (java.io.FileNotFoundException e) {
      return false;
    }
  }

  /**
   * Monitor the file in the primary cache directory. when can't read from, will switch; when ok,
   * will switch back.
   *
   * @param interval
   */
  private void startCacheListener(final long interval) {
    ExecutorService pool = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("cache-listener").build());

    pool.submit(new Runnable() {
      @Override
      public void run() {
        while (true) {
          boolean ev = false;
          BufferedReader br = null;
          try {
            File f = new File(basedir, "deadbeaf.deadbeaf");
            if (!f.exists()) {
              ev = true;
            } else {
              br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
              String line = br.readLine();
              if (line == null) {
                ev = true;
              }
              if (!"deadbeaf".equals(line)) {
                ev = true;
              }
            }
          } catch (java.io.FileNotFoundException e) {
            ev = true;
          } catch (IOException e) {
            ev = true;
          } finally {
            if (br != null) {
              try {
                br.close();
              } catch (IOException e) {
                ev = true;
              }
            }
            long result = System.currentTimeMillis();
            result = result / 1000 * 1000;
            if (ev) {
              result += 1;
            }
            sc.onSwitch(result);
          }
          try {
            Thread.sleep(interval);
          } catch (InterruptedException e) {
          }
        }
      }
    });
    MoreExecutors.addDelayedShutdownHook(pool, DfsConstants.THREADPOOL_SHUTDOWN_AWAIT_SECONDS,
        TimeUnit.SECONDS);
  }
}
