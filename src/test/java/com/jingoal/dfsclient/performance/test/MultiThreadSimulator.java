package com.jingoal.dfsclient.performance.test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiThreadSimulator {

  private static final Logger logger = LoggerFactory.getLogger(MultiThreadSimulator.class);

  private int threadSize = 10;

  private int taskSize = 10;

  private int minFileLength = 1; // byte

  private int maxFileLength = 1024 * 1024 * 2; // 2m

  private ExecutorService executor;

  private Random random = new Random();

  public MultiThreadSimulator(int threadSize, int taskSize, int minFileLength, int maxFileLength) {
    super();
    if (threadSize > 0) {
      this.threadSize = threadSize;
    }
    if (minFileLength > 0) {
      this.minFileLength = minFileLength;
    }
    if (maxFileLength > 0) {
      this.maxFileLength = maxFileLength;
    }

    this.taskSize = taskSize;


    executor = new ThreadPoolExecutor(1, threadSize, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(threadSize * 2), new ThreadPoolExecutor.CallerRunsPolicy());
  }

  public void start() {
    InputStreamGenerator.init(maxFileLength);

    for (int i = 0; taskSize <= 0 || i < taskSize; i++) {
      submitTask(i);

      try {
        Thread.sleep(random.nextInt(100) + 1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    logger.info("All task create complete.");
  }

  private void submitTask(int i) {
    int fileLength = getFileLength();
    Runnable processor = new BizTask(fileLength, 10L, "PERF-TEST", i + "", 90L);
    executor.submit(processor);
  }

  private int getFileLength() {
    return random.nextInt((maxFileLength - minFileLength) + 1) + minFileLength;
  }

  public void stop() {
    if (null != executor) {
      executor.shutdown();
    }
  }

  public int getThreadSize() {
    return threadSize;
  }

  public int getMinFileLength() {
    return minFileLength;
  }

  public void setMinFileLength(int minFileLength) {
    this.minFileLength = minFileLength;
  }

  public int getMaxFileLength() {
    return maxFileLength;
  }

  public void setMaxFileLength(int maxFileLength) {
    this.maxFileLength = maxFileLength;
  }
}
