package com.jingoal.dfsclient.performance.test;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class PrometheusMetrics {

  public static final Gauge duplicateGauge = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("duplicate_time").help("'Duplicate' method consume time in milliseconds.").register();

  public static final Gauge copyGauge = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("copy_time").help("'Copy' method consume time in milliseconds.").register();

  public static final Gauge existByMd5Gauge = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("existByMd5_time").help("'ExistByMd5' method consume time in milliseconds.").register();

  public static final Gauge getByMd5Gauge = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("getByMd5_time").help("'GetByMd5' method consume time in milliseconds.").register();

  public static final Gauge existGauge = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("exist_time").help("'Exist'method consume time in milliseconds.").register();

  public static final Gauge getFileInfoGauge =
      Gauge.build().namespace("dfs2_0").subsystem("perfcln").name("getFileInfo_time")
          .help("'GetFileInfo' method consume time in milliseconds.").register();

  public static final Gauge deleteGauge = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("delete_time").help("'Delete' method consume time in milliseconds.").register();

  public static final Gauge kbpsPutFile = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("putFile_KBPS").help("Translate rate in KByte/sec of 'putFile'.").register();

  public static final Gauge kbpsGetFile = Gauge.build().namespace("dfs2_0").subsystem("perfcln")
      .name("getFile_KBPS").help("Translate rate in KByte/sec of 'getFile'").register();

  public static final Counter invokedCounter =
      Counter.build().namespace("dfs2_0").subsystem("perfcln").name("method_invoke_count")
          .help("The counter that every method has invoked.").labelNames("service").register();
}
