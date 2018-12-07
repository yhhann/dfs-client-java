package com.jingoal.dfsclient.performance.test;

import static spark.Spark.get;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.DFSClientImplV20;
import com.jingoal.dfsclient.DFSShardClientV20;
import com.jingoal.dfsclient.cache.LocalDiskCache;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import spark.Spark;

/**
 * dfsClient多线程性能测试，测试数据保存在prometheus提供的度量模型中
 *
 * @author zhaochl
 *
 */
public class DFSClientPerfTest {
  private static final Logger logger = LoggerFactory.getLogger(DFSClientPerfTest.class);
  private static DFSShardClientV20 client;

  // 5 10 1024 2097152 dfslb://192.168.37.6:10000 4567
  public static void main(String[] args) {
    if (args == null || args.length < 6) {
      System.out.println(
          "Enter the parameters in the following format : "
          + "'threadSize taskSize minFileLength maxFileLength dfsUri metrics-port', "
          + "for example :'5 100 1024 2048 dfslb://192.168.37.6:10000 4567'");
      System.exit(0);
    }
    int threadSize = Integer.parseInt(args[0]);
    int taskSize = Integer.parseInt(args[1]);
    int minFileLength = Integer.parseInt(args[2]);
    int maxFileLength = Integer.parseInt(args[3]);
    String seeds = args[4]; // dfslb://192.168.37.6:10000
    int sparkPort = Integer.parseInt(args[5]); // 4567

    try {
      initDFSClient(seeds);
    } catch (Exception e) {
      logger.error("Init dfsHandler occur exception.", e);
    }

    startPrometheusServer(sparkPort);

    MultiThreadSimulator simulator =
        new MultiThreadSimulator(threadSize, taskSize, minFileLength, maxFileLength);
    simulator.start();
  }

  public static void initDFSClient(String seeds) throws Exception {
    client = new DFSClientImplV20(seeds, "perf-cln",
        new LocalDiskCache(System.getProperty("java.io.tmpdir") , 5000));
  }

  public static void startPrometheusServer(int port) {
    Spark.port(port);

    get("/metrics", (req, res) -> {
      res.status(HttpServletResponse.SC_OK);
      res.type(TextFormat.CONTENT_TYPE_004);

      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
      PrintWriter writer = new PrintWriter(baos);
      TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
      writer.flush();
      writer.close();

      return baos.toString();
    });
  }

  public static DFSShardClientV20 getClient() {
    return client;
  }
}
