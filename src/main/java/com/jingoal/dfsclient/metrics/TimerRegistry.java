package com.jingoal.dfsclient.metrics;

import java.util.concurrent.atomic.AtomicInteger;

import com.codahale.metrics.Timer;

/**
 * A thread's Timer.Context is assigned the first time it invokes {@code timer.time()} and remains
 * unchanged on subsequent calls.
 */
public class TimerRegistry {

  private static final ThreadLocal<ContextRef> threadLocal = new ThreadLocal<ContextRef>();

  /** Tick the clock. */
  public static void startTimer(final Timer timer) {
    ContextRef ctxRef = threadLocal.get();
    if (ctxRef == null) {
      ctxRef = new ContextRef(timer);
      threadLocal.set(ctxRef);
    }
    ctxRef.chkOrTick();
  }

  /** Metrics the timer elapsed. */
  public static void stopTimer() {
    ContextRef ctxRef = threadLocal.get();
    if (ctxRef != null) {
      if (ctxRef.chkOrStop()) {
        threadLocal.remove();
      }
    }
  }

  static class ContextRef {

    private Timer timer;
    private Timer.Context context;
    private AtomicInteger ref;

    public ContextRef(final Timer timer) {
      this.timer = timer;
    }

    /**
     * Create Timer.Context when the current thread does not hold one; otherwise, increment the
     * reference.
     */
    public void chkOrTick() {
      if (context == null) {
        context = timer.time();// will tick clock.
        ref = new AtomicInteger(1);
      } else {
        ref.incrementAndGet();
      }
    }

    /**
     * Stop the Timer.Context when the current thread's reference is zero; otherwise decrement the
     * reference.
     */
    public boolean chkOrStop() {
      if (context == null || ref == null) {
        return false;
      }
      if (ref.decrementAndGet() == 0) {
        context.stop();// will update elapsed.
        return true;
      } else {
        return false;
      }
    }
  }
}
