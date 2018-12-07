package com.jingoal.dfsclient.client;

import java.net.InetSocketAddress;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.discovery.DfsServer;
import com.jingoal.dfsclient.util.UriUtils;

public class ServerDiscoveryTest {
  private static final Logger logger = LoggerFactory.getLogger(ServerDiscoveryTest.class);

  @Test
  public void testRefreshLoop() {
    ServerDiscovery sd =
        new ServerDiscovery(new InetSocketAddress("192.168.5.239", 10000), "server-discovery-test");
    sd.refreshLoop(new ServerListener() {

      @Override
      public void onChange(final List<DfsServer> servers) {

        logger.info("RefreshLoop test, servers size onchange: " + servers.size());
        for (DfsServer server : servers) {
          InetSocketAddress addr = UriUtils.getSocketAddress(server.getUri());
          logger.info("RefreshLoop test, node: " + addr);
        }
      }
    });
  }
}
