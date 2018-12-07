package com.jingoal.dfsclient.metrics;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;

public class MetricsHandler {

  public static final MetricsHandler getInstance() {
    return SingletonHolder.INSTANCE;
  }

  private static class SingletonHolder {
    private static final MetricsHandler INSTANCE = new MetricsHandler();
  }

  private static final MetricRegistry metrics = new MetricRegistry();
  private static Reporter reporter = null;

  private MetricsHandler() {
    init();
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        MetricsHandler.this.stopMetrics();
      }
    });
  }

  /** Build and start the metrics Reporter. */
  private void init() {
    if (!MetricsCfg.isMetricsEnable()) {
      return;
    }
    if (MetricsCfg.getReporter().equals("JMX")) {
      reporter = JmxReporter.forRegistry(metrics).build();
      ((JmxReporter) reporter).start();
    } else {
      reporter = Slf4jReporter.forRegistry(metrics).build();
      ((Slf4jReporter) reporter).start(MetricsCfg.getReportIntervalInSecond(), TimeUnit.SECONDS);
    }
  }

  /** Stop the metrics Reporter. */
  public void stopMetrics() {
    if (reporter == null) {
      return;
    }
    if (reporter instanceof JmxReporter) {
      ((JmxReporter) reporter).stop();
    } else if (reporter instanceof Slf4jReporter) {
      ((Slf4jReporter) reporter).stop();
    }
    reporter = null;
  }

  /**
   * Start the Timer.
   *
   * @param name Timer name.
   */
  public void startTimer(final String name) {
    if (!MetricsCfg.isMetricsEnable()) {
      return;
    }
    Timer timer = metrics.timer(name);
    TimerRegistry.startTimer(timer);
  }

  /** Stop the Timer. */
  public void stopTimer() {
    if (!MetricsCfg.isMetricsEnable()) {
      return;
    }
    TimerRegistry.stopTimer();
  }

  /**
   * Increment the Counter.
   *
   * @param name Counter name.
   */
  public void incCounter(final String name) {
    if (!MetricsCfg.isMetricsEnable()) {
      return;
    }
    metrics.counter(name).inc();
  }

  /**
   * Update the Histogram with the given value.
   *
   * @param name Histogram name.
   * @param value The update value.
   */
  public void updateHistogram(final String name, final long value) {
    if (!MetricsCfg.isMetricsEnable()) {
      return;
    }
    metrics.histogram(name).update(value);
  }

  /**
   * Returns the value at the given quantile.
   *
   * @param name Histogram name.
   * @param quantile a given quantile, in {@code [0..1]}
   * @return the value in the distribution at {@code quantile}
   */
  public double getValue(final String name, final double quantile) {
    if (!MetricsCfg.isMetricsEnable()) {
      return 0.0;
    }
    return metrics.histogram(name).getSnapshot().getValue(quantile);
  }
}
