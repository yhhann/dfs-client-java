package com.jingoal.dfsclient;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jingoal.dfsclient.util.ThreadPoolUtils;

public class DfsPutFileTest extends AbstractHandlerTest {
  private static final Logger logger = LoggerFactory.getLogger(DfsPutFileTest.class);

  private int poolsize = 5;
  private long domain = 1;
  private long dstdomain = 2;
  private String localDir = System.getProperty("java.io.tmpdir");

  @Test
  public void test() {
    ExecutorService pool = null;
    try {
      pool = Executors.newFixedThreadPool(poolsize,
          new ThreadFactoryBuilder().setDaemon(true).setNameFormat("dfs-%d").build());
      doExecute(pool);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    } finally {
      if (pool != null) {
        ThreadPoolUtils.shutdownAndAwaitTermination(pool);
      }
    }
  }

  private void doExecute(final ExecutorService pool) {
    logger.info("Execute begin...");
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
    handleConcurrentTasks(pool, fileArray);

    logger.info("Execute complete.");
  }

  private void handleConcurrentTasks(final ExecutorService pool, final File[] fileArray) {
    List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
    for (File file : fileArray) {
      tasks.add(new DfsPutFileTask(domain, file));
    }
    try {
      pool.invokeAll(tasks);
      tasks.clear();
      tasks = null;
    } catch (InterruptedException e) {
    }
  }

  class DfsPutFileTask implements Callable<Void> {
    private long domain;
    private File file;

    public DfsPutFileTask(final long domain, final File file) {
      this.domain = domain;
      this.file = file;
    }

    @Override
    public Void call() throws Exception {
      ThreadPoolUtils.process(file.getName(), new ThreadPoolUtils.ThreadProcessor() {

        @Override
        public void process() {
          try {
            doCall();
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          } finally {
            logger.info(file.getName() + " handle complete.");
          }
        }
      });

      return null;
    }

    private void doCall() throws Exception {
      DFSShardClientV20 client = getClient();
      logger.info("Call putFile, put file {}", file.getName());
      String fid = client.putFileWithoutMove(domain, "dfs-client", file.getName(), file, "10010");
      logger.info("Successed putFile file {} complete, return fid:{}", file.getName(), fid);

      logger.info("Call getFile, fid:{} domain:{}", fid, domain);
      File file = client.getFile(fid, domain);
      logger.info("Successed getFile, {}", getFileInfo(file));

      String dupId = client.copy(domain, "dfs-client", domain, fid, "10010");
      logger.info(
          "Same domain copy, srcdomain:{} dstdomain:{} srcfid:{}, will duplicate, dup id:{}",
          domain, domain, fid, dupId);

      boolean delFlag = client.delete(dupId, domain);
      logger.info("Delete duplicate, dupId:{} domain:{} success:{}", dupId, domain, delFlag);

      boolean isExist = client.exist(dupId, domain);
      logger.info("After duplicate deleted, dupId:{} exist:{}", dupId, isExist);

      String cid = client.copy(dstdomain, "dfs-client", domain, fid, "10010");
      logger.info("Not same domain copy, srcdomain:{} dstdomain:{} srcfid:{}, return fid:{}",
          domain, dstdomain, fid, cid);

      isExist = client.exist(cid, dstdomain);
      logger.info("Copy fid:{} domain:{} exist:{}", cid, dstdomain, isExist);

      delFlag = client.delete(cid, dstdomain);
      logger.info("Delete copy,  fid:{} domain:{} success:{}", cid, dstdomain, delFlag);

      isExist = client.exist(cid, dstdomain);
      logger.info("After deleted copy, fid:{} domain:{} exist:{}", cid, dstdomain, isExist);

      delFlag = client.delete(fid, domain);
      logger.info("Delete source, fid:{} domain:{} success:{}", fid, domain, delFlag);

      isExist = client.exist(fid, domain);
      logger.info("After deleted source, fid:{} domain:{} exist:{}", fid, domain, isExist);
    }
  }
}
