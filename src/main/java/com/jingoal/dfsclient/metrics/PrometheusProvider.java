package com.jingoal.dfsclient.metrics;

import com.jingoal.dfsclient.util.TimerUtils;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

public class PrometheusProvider {

  // according instance,not add label.
  public static final Counter cacheSpinCounter = Counter.build().namespace("dfs2_0")
      .subsystem("client").name("cache_spin_count").help("Cache spin counter.").register();

  public static final Gauge updateServerListElapse =
      Gauge.build().namespace("dfs2_0").subsystem("client").name("update_server_list_elapse")
          .help("Update server list elapse in milliseconds.").labelNames("level").register();

  // according instance,not add label.
  public static final Counter accessCounter = Counter.build().namespace("dfs2_0")
      .subsystem("client").name("access_count").help("Access count.").register();

  public static final Counter loseHandlerCounter = Counter.build().namespace("dfs2_0")
      .subsystem("client").name("lose_handler_count").help("Lose handler count.").register();

  public static final Gauge accessTime = Gauge.build().namespace("dfs2_0").subsystem("client")
      .name("access_time").help("Access in milliseconds.").labelNames("service").register();

  public static final Counter failCounter = Counter.build().namespace("dfs2_0").subsystem("client")
      .name("fail_count").help("Fail count.").labelNames("service").register();

  public static final Counter timeoutCounter =
      Counter.build().namespace("dfs2_0").subsystem("client").name("timeout_count")
          .help("Timeout count.").labelNames("service").register();

  public static final Gauge timeoutGauge = Gauge.build().namespace("dfs2_0").subsystem("client")
      .name("timeout_time").help("Timeout in milliseconds.").labelNames("service").register();

  public static void timeoutStat(String service, TimerUtils timer) {
    PrometheusProvider.timeoutCounter.labels(service).inc();
    PrometheusProvider.timeoutGauge.labels(service).set(timer.elapsedMillis());
  }

  public static final Gauge kbpsGauge = Gauge.build().namespace("dfs2_0").subsystem("client")
      .name("KBPS_value").help("Translate rate in KByte/sec.").labelNames("service").register();

  public static final Gauge filesizeGauge = Gauge.build().namespace("dfs2_0").subsystem("client")
      .name("file_size").help("File size in bytes.").labelNames("service", "biz").register();

  // add counter instance for record total number of access cache
  public static final Counter cacheTotalCounter = Counter.build().namespace("dfs2_0")
     .subsystem("client").name("cache_total_count").help("Access cache total count.").register();

  // add counter instance for record hit number of access cache
  public static final Counter cacheHitCounter = Counter.build().namespace("dfs2_0")
    .subsystem("client").name("cache_hit_count").help("Access cache hit count.").register();

  public static final Counter writeCacheFailedCounter = Counter.build().namespace("dfs2_0").
      subsystem("client").name("write_cache_failed_count").help("Write cache fail count.")
      .register();
}