package com.jingoal.dfsclient.test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer and consumer scheduler.
 *
 * @author wangjie
 *
 * @param <E>
 */
public abstract class WorkDeque<E> {
  private static final Logger logger = LoggerFactory.getLogger(WorkDeque.class);

  private final BlockingDeque<E> deque = new LinkedBlockingDeque<E>();
  /** consumer batch size. */
  private int batch;

  public WorkDeque(final int arg) {
    this.batch = arg;
  }

  /**
   * Producer put item into deque, when capacity greater than batch, will invoke consume.
   *
   * @param item
   */
  public final void enqueue(final E item) {
    try {
      deque.put(item);

      Collection<E> list = new LinkedList<E>();
      if (deque.size() >= batch) {
        deque.drainTo(list, batch);
        process(list);
        list.clear();
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  /**
   * When scheduler end, clean the deque.
   */
  public final void clean() {
    if (deque.size() > 0) {
      Collection<E> list = new LinkedList<E>();
      while (deque.size() >= batch) {
        deque.drainTo(list, batch);
        process(list);
        list.clear();
      }
      deque.drainTo(list);
      if (list.size() > 0) {
        process(list);
        list.clear();
      }
    }
  }

  /**
   * Consumer process.
   *
   * @param list Item collections.
   */
  protected abstract void process(Collection<E> list);
}
