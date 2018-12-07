package com.jingoal.dfsclient.test;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.DFSShardClientV20;
import com.jingoal.dfsclient.util.ThreadPoolUtils;

public class PutDirProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PutDirProcessor.class);

  private ExecutorService pool;
  private DFSShardClientV20 client;
  private WorkDeque<String> deque;
  private long domain;// 公司ID
  private int batch;// 写入文件批次个数

  public void process(final File[] fileArray) {
    Collection<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
    int num = 0;
    for (File file : fileArray) {
      tasks.add(new PutFileTask(file));
      num++;
      if (num % batch == 0) {
        try {
          pool.invokeAll(tasks);
          tasks.clear();
          num = 0;
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
        }
      }
    }
    if (tasks.size() > 0) {
      try {
        pool.invokeAll(tasks);
        tasks.clear();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
    tasks = null;
  }

  public void setPool(final ExecutorService pool) {
    this.pool = pool;
  }

  public void setClient(final DFSShardClientV20 client) {
    this.client = client;
  }

  public void setDeque(final WorkDeque<String> deque) {
    this.deque = deque;
  }

  public void setDomain(final long domain) {
    this.domain = domain;
  }

  public void setBatch(final int batch) {
    this.batch = batch;
  }

  class PutFileTask implements Callable<Void> {

    private File file;

    public PutFileTask(final File file) {
      this.file = file;
    }

    @Override
    public Void call() throws Exception {
      ThreadPoolUtils.process(file.getName(), new ThreadPoolUtils.ThreadProcessor() {

        @Override
        public void process() {
          logger.info("Start upload file {}...", file.getName());
          try {
            String fid =
                client.putFileWithoutMove(domain, "dfs-test", file.getName(), file, "10010");
            deque.enqueue(fid);
            logger.info("File {} upload success, fid:{} ", file.getName(), fid);
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      });

      return null;
    }
  }
}
