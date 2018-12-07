package com.jingoal.dfsclient.util;

/**
 * 常量配置
 *
 */
public final class DfsConstants {

  private DfsConstants() {}

  /** Wait a while for thread pool to terminate. */
  public static final Long THREADPOOL_SHUTDOWN_AWAIT_SECONDS = 60L;
  /** Wait a while for channel to terminate. */
  public static final Long CHANNEL_SHUTDOWN_AWAIT_SECONDS = 30L;

  /** GRPC channel chunk size in bytes, default 256KB. */
  public static long getDefaultChunkSizeInBytes() {
    return Long.parseLong(System.getProperty("dfs.chunk.size.in.bytes", "262144"));
  }

  /** Stream operation minimum timeout in milliseconds. */
  public static long getMinStreamTimeOut() {
    return Long.parseLong(System.getProperty("dfs.stream.timeout", "60000"));
  }

  /** Read operation default timeout in milliseconds. */
  public static long getDefaultReadTimeOut() {
    return Long.parseLong(System.getProperty("dfs.read.timeout", "30000"));
  }

  public static final Long KB = 1024L;// k bytes
  public static final Long MB = 1024 * 1024L;
  public static final Long GB = 1024 * 1024 * 1024L;

  /** DEADLINE switch */
  public static boolean isDeadlineEnable() {
    return "true".equals(System.getProperty("dfs.deadline.enable", "true"));
  }

  /** DEADLINE compute factor. */
  public static double getDeadlineAdjustFactor() {
    return Double.parseDouble(System.getProperty("dfs.deadline.adjust.factor", "2.0"));
  }

  /**
   * Flow control switch
   */
  public static boolean isFlowControlEnabled() {
    return "true".equals(System.getProperty("dfs.flow.control.enable", "true"));
  }

  /**
   * Timeout to wait for flow control, in millisecond.
   */
  public static int timeoutToWaitFlowControl() {
    return Integer.parseInt(System.getProperty("dfs.flow.control.timeout", "5"));
  }

  public static final String METRICS_CACHESPIN = "CacheSpin";
  public static final String METRICS_GETFILE = "GetFile";
  public static final String METRICS_PUTFILE = "PutFile";
  public static final String METRICS_DELETE = "RemoveFile";
  public static final String METRICS_DUPLICATE = "Duplicate";
  public static final String METRICS_COPY = "Copy";
  public static final String METRICS_GETBYMD5 = "GetByMd5";
  public static final String METRICS_EXISTBYMD5 = "ExistByMd5";
  public static final String METRICS_EXIST = "Exists";
  public static final String METRICS_UPDATELEVEL1 = "UpdateLevel1";
  public static final String METRICS_UPDATELEVEL0 = "UpdateLevel0";
}
