package com.jingoal.dfsclient.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Thread pool utils.
 */
public final class ThreadPoolUtils {

  private ThreadPoolUtils() {}

  /**
   * Modify the given thread name for custom.
   *
   * @param threadName
   * @param processor
   */
  public static void process(final String threadName, final ThreadProcessor processor) {
    final Thread currentThread = Thread.currentThread();
    final String oldName = currentThread.getName();
    currentThread.setName("Processing-" + threadName);
    try {
      processor.process();
    } finally {
      currentThread.setName(oldName);
    }
  }

  /**
   * The processor for custom thread name.
   */
  public interface ThreadProcessor {

    public void process();
  }

  /**
   * Stop the thread pool gracefully.
   *
   * @param pool the thread pool
   */
  public static void shutdownAndAwaitTermination(final ExecutorService pool) {
    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        List<Runnable> rejected = pool.shutdownNow(); // Cancel waiting tasks
        System.err.println("Rejected tasks: " + rejected.size());
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
          System.err.println("Pool did not terminate");
        }
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }

  public static void sleep(final long millis) {
    try {
      TimeUnit.MILLISECONDS.sleep(millis);
    } catch (InterruptedException e) {
    }
  }

  public static void waitOneSeconds() {
    try {
      TimeUnit.SECONDS.sleep(1);;
    } catch (InterruptedException e) {
    }
  }

  public static void waitSeconds(final int bound) {
    try {
      TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(bound));
    } catch (InterruptedException e) {
    }
  }
}
