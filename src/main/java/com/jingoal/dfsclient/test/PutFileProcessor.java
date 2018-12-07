package com.jingoal.dfsclient.test;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.DFSOutputStream;
import com.jingoal.dfsclient.DFSShardClientV20;
import com.jingoal.dfsclient.util.FileUtils;
import com.jingoal.dfsclient.util.RandomStringUtils;
import com.jingoal.dfsclient.util.ThreadPoolUtils;

public class PutFileProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PutFileProcessor.class);

  private ExecutorService pool;
  private DFSShardClientV20 client;
  private WorkDeque<String> deque;
  private long domain;// 公司ID
  private int count;// 生成文件数量
  private int length;// 生成文件大小限制
  private int batch;// 写入文件批次个数

  public void process() {
    Collection<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
    int num = 0;
    for (int i = 0; i < count; i++) {
      tasks.add(new PutStreamTask());
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

  public void setCount(final int count) {
    this.count = count;
  }

  public void setLength(final int length) {
    this.length = length;
  }

  public void setBatch(final int batch) {
    this.batch = batch;
  }

  class PutStreamTask implements Callable<Void> {

    @Override
    public Void call() throws Exception {
      String filename = RandomStringUtils.randomAlphabetic(6);// 生成随机文件名称
      ThreadPoolUtils.process(filename, new ThreadPoolUtils.ThreadProcessor() {

        @Override
        public void process() {
          logger.info("Start upload file {}...", filename);
          try {
            Random rand = new Random();
            byte[] byteArray = new byte[rand.nextInt(length)];
            DFSOutputStream output =
                client.getOutputStream(domain, "dfs-test", filename, "10010", length);
            FileUtils.copy(new ByteArrayInputStream(byteArray), output);// will close stream
            deque.enqueue(output.getId());
            logger.info("File {} upload success, fid:{}, length:{}", filename, output.getId(),
                output.getLength());
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      });

      return null;
    }
  }
}
