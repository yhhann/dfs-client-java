package com.jingoal.dfsclient.util;

public class TimerUtils {
  /**
   * Number of nanoseconds in a second.
   */
  public static final double NANOSECONDS_PER_SECOND = 1E9;
  /**
   * Number of nanoseconds in a milliseconds.
   */
  public static final double NANOSECONDS_PER_MILLISECONDS = 1E6;

  private final long start;
  static TimeProvider defaultTimeProvider = new TimeProvider();
  private final TimeProvider timeProvider;

  static class TimeProvider {
    long nanoTime() {
      return System.nanoTime();
    }
  }

  // Visible for testing.
  TimerUtils(TimeProvider timeProvider) {
    this.timeProvider = timeProvider;
    start = timeProvider.nanoTime();
  }

  public TimerUtils() {
    this(defaultTimeProvider);
  }

  /**
   * @return Measured duration in seconds since {@link TimerUtils} was constructed.
   */
  public double elapsedSeconds() {
    return elapsedSecondsFromNanos(start, timeProvider.nanoTime());
  }

  public static double elapsedSecondsFromNanos(long startNanos, long endNanos) {
    return (endNanos - startNanos) / NANOSECONDS_PER_SECOND;
  }

  /**
   * @return Measured duration in milliseconds since {@link TimerUtils} was constructed.
   */
  public double elapsedMillis() {
    return elapsedMillisFromNanos(start, timeProvider.nanoTime());
  }

  public static double elapsedMillisFromNanos(long startNanos, long endNanos) {
    return (endNanos - startNanos) / NANOSECONDS_PER_MILLISECONDS;
  }
}
