package com.jingoal.dfsclient.metrics;

/**
 * Metrics config.
 */
public final class MetricsCfg {

  private MetricsCfg() {}

  /** Reporter type. JMX or Slf4j */
  public static String getReporter() {
    return System.getProperty("dfs.metrics.reporter", "JMX");
  }

  /** Metrics switch. */
  public static boolean isMetricsEnable() {
    return "true".equals(System.getProperty("dfs.metrics.enable", "true"));
  }

  /** Starts the Slf4jReporter polling at the given period. */
  public static int getReportIntervalInSecond() {
    return Integer.parseInt(System.getProperty("dfs.metrics.interval.in.second", "30"));
  }
}
