package com.jingoal.dfsclient.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.discovery.DfsServer;
import com.jingoal.dfsclient.util.UriUtils;

public class ServerMonitorTest {
  private static final Logger logger = LoggerFactory.getLogger(ServerMonitorTest.class);

  private static ServerMonitor monitor;

  @BeforeClass
  public static void setUp() {
    List<InetSocketAddress> seeds = new ArrayList<InetSocketAddress>();
    seeds.add(new InetSocketAddress("192.168.64.176", 10000));
    seeds.add(new InetSocketAddress("192.168.64.176", 10001));
    seeds.add(new InetSocketAddress("192.168.64.176", 10003));

    monitor = new ServerMonitor(seeds, "server-monitor-test");
  }

  @Test
  public void testStart() {
    CountDownLatch latch = new CountDownLatch(10);

    try {
      monitor.start(new ServerListener() {

        @Override
        public void onChange(final List<DfsServer> servers) {
          List<InetSocketAddress> level0 = new ArrayList<InetSocketAddress>();
          ConcurrentMap<Long, InetSocketAddress> level1 =
              new ConcurrentHashMap<Long, InetSocketAddress>();

          for (DfsServer server : servers) {
            InetSocketAddress addr = UriUtils.getSocketAddress(server.getUri());

            if (server.getPreferredCount() == 0) { // level 0
              level0.add(addr);
              logger.warn("Succeeded to add DFS server {}.", addr);
            } else { // level 1
              for ( String preferred : server.getPreferredList()) {
                try {
                  long domain = Long.parseLong(preferred);
                  level1.put(domain, addr);
                  logger.warn("Succeeded to add preferred server {} -> {}.", domain, addr);
                } catch (NumberFormatException e) {
                  logger.warn("Invalid format of preferr {}", preferred);
                }
              }
            }
          }

          latch.countDown();
        }
      });
    } catch (Exception e) {
      latch.countDown();
      e.printStackTrace();
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
    }
  }
}
