package com.jingoal.dfsclient.client;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jingoal.dfsclient.discovery.DfsServer;
import com.jingoal.dfsclient.util.DfsConstants;
import com.jingoal.dfsclient.util.StringUtils;
import com.jingoal.dfsclient.util.UriUtils;

/**
 * ServerMonitor 用来监控服务器列表的变化。
 *
 * @author yhhan
 */
public class ServerMonitor {
  private static final Logger logger = LoggerFactory.getLogger(ServerMonitor.class);

  private static Iterator<InetSocketAddress> addrIter = null;

  private List<InetSocketAddress> seeds = new ArrayList<InetSocketAddress>();
  private String clientId;

  private ServerDiscovery sd;
  private ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture<?> sFuture;

  /**
   *
   * @param seeds DFS Server 的种子列表。
   * @param clientId 客户端的唯一id
   */
  public ServerMonitor(final List<InetSocketAddress> seeds, final String clientId) {
    this.clientId = clientId;
    this.seeds.addAll(seeds);
    addrIter = new CycleIterator<InetSocketAddress>(this.seeds);
  }

  /**
   * start 初始化 ServerMonitor, 如果给定的 seed 都不可用, 会持续尝试连接。
   * 当 Server 的列表发生变化时， 会调用 listener 的 onChange() 方法。
   *
   * @throws ServerMonitorException
   */
  public void start(final ServerListener listener) throws ServerMonitorException {
    if (listener == null) {
      throw new ServerMonitorException("Listener can not be null.");
    }

    CountDownLatch latch = new CountDownLatch(1);

    this.scheduledExecutorService = Executors.newScheduledThreadPool(1,
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("monitor-%d").build());
    this.sFuture = this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {

      @Override
      public void run() {
        int errCnt = 0;
        while (addrIter.hasNext()) {
          InetSocketAddress addr = addrIter.next();

          try {
            if (sd != null) {
              sd.shutdown();
            }
            sd = new ServerDiscovery(addr, clientId);

            sd.refreshLoop(new ServerListener() {

              @Override
              public void onChange(final List<DfsServer> servers) {
                List<InetSocketAddress> serverAddrs = new ArrayList<InetSocketAddress>();

                for (DfsServer server : servers) {
                  InetSocketAddress addr = UriUtils.getSocketAddress(server.getUri());
                  serverAddrs.add(addr);
                }
                listener.onChange(servers);

                addrIter = new CycleIterator<InetSocketAddress>(serverAddrs);
                latch.countDown();
              }

            });
          } catch (Exception e) {
            String msg = e.getMessage();
            if (e.getCause() != null && StringUtils.isNotBlank(e.getCause().getMessage())) {
              msg = e.getCause().getMessage();
            }
            logger.warn("Failed to discovery from {}, {}", addr,  msg);

            Collection<InetSocketAddress> currAddrs =
                ((CycleIterator<InetSocketAddress>) addrIter).Underlying();
            if (++errCnt > currAddrs.size() - 1 || currAddrs.size() == 0) {
              Set<InetSocketAddress> addrSet = new HashSet<InetSocketAddress>(seeds);
              addrSet.addAll(currAddrs);

              addrIter = new CycleIterator<InetSocketAddress>(
                  new ArrayList<InetSocketAddress>(addrSet));
              break;
            }

            try {
              Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
          }
        }
      }

    }, 0, 3, TimeUnit.SECONDS);

    try {
      latch.await();
    } catch (InterruptedException e) {
      logger.warn("Must check if servers are discoveried.");
    }

    MoreExecutors.addDelayedShutdownHook(this.scheduledExecutorService,
        DfsConstants.THREADPOOL_SHUTDOWN_AWAIT_SECONDS, TimeUnit.SECONDS);
  }

  public void shutdown() {
    if (this.sFuture != null && !this.sFuture.isDone()) {
      this.sFuture.cancel(true);
    }

    if (this.scheduledExecutorService != null && !this.scheduledExecutorService.isShutdown()) {
      this.scheduledExecutorService.shutdownNow();
    }

    if (sd != null) {
      try {
        sd.shutdown();
      } catch (InterruptedException ignored) {
      }
    }
  }

}
