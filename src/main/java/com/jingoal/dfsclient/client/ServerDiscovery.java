package com.jingoal.dfsclient.client;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jingoal.dfsclient.discovery.DfsClient;
import com.jingoal.dfsclient.discovery.DfsServer;
import com.jingoal.dfsclient.discovery.DiscoveryServiceGrpc;
import com.jingoal.dfsclient.discovery.GetDfsServersRep;
import com.jingoal.dfsclient.discovery.GetDfsServersRep.GetDfsServerUnionCase;
import com.jingoal.dfsclient.discovery.GetDfsServersReq;
import com.jingoal.dfsclient.util.DfsConstants;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * 访问 gRPC 的 java 客户端，处理 discovery 相关的业务。
 *
 * @author yhhan
 */
public class ServerDiscovery {
  private static final Logger logger = LoggerFactory.getLogger(ServerDiscovery.class);
  private static ExecutorService pool = Executors.newFixedThreadPool(1,
      new ThreadFactoryBuilder().setDaemon(true).setNameFormat("discovery-%d").build());
  private final ManagedChannel channel;
  private final DiscoveryServiceGrpc.DiscoveryServiceBlockingStub blockingStub;
  private final Iterator<GetDfsServersRep> iter;
  private HB hb = new HB();
  private InetSocketAddress addr;

  public ServerDiscovery(final InetSocketAddress addr, final String clientId) {
    this.addr = addr;
    channel = ManagedChannelBuilder.forAddress(addr.getHostName(), addr.getPort())
        .usePlaintext(true).build();
    blockingStub = DiscoveryServiceGrpc.newBlockingStub(channel);
    iter = blockingStub.getDfsServers(GetDfsServersReq.newBuilder()
        .setClient(DfsClient.newBuilder().setId(clientId).build()).build());
  }

  /**
   * Initiates an orderly shutdown in which preexisting calls continue but new calls are immediately
   * cancelled.
   *
   * @throws InterruptedException
   */
  public void shutdown() throws InterruptedException {
    if (channel != null) {
      channel.shutdown().awaitTermination(DfsConstants.CHANNEL_SHUTDOWN_AWAIT_SECONDS,
          TimeUnit.SECONDS);
    }
  }

  public void refreshLoop(final ServerListener listener) {
    Future<String> f = pool.submit(new Callable<String>() {
      @Override
      public String call() {
        while (iter.hasNext()) {
          GetDfsServersRep rep = iter.next();
          List<DfsServer> ss = processReply(rep);
          if (ss != null && ss.size() > 0) {
            listener.onChange(ss);
          }
        }
        return "";
      }
    });

    while (true) {
      try {
        f.get(500, TimeUnit.MILLISECONDS);
      } catch (ExecutionException e) {
        logger.debug("execution exception.", e.getCause());
        throw new DiscoveryException("discovery error", e.getCause());
      } catch (InterruptedException ignored) {
        // ignored.
      } catch (TimeoutException ignored) {
        // it's ok.
      }

      hb.HbCheckUp();
    }

  }

  private List<DfsServer> processReply(final GetDfsServersRep rep) {
    GetDfsServerUnionCase c = rep.getGetDfsServerUnionCase();
    switch (c.getNumber()) {
      case 1:
        return rep.getSl().getServerList();
      case 2:
        hb.HbUpdate();
        break;
      default:
        logger.warn("Not known number:" + c.getNumber());
    }
    return null;
  }

  @Override
  public String toString() {
    return this.channel.authority();
  }

  class HB {
    private static final long DefaultInterval = 3000; // ms

    long interval;
    long p;
    long n;

    void HbUpdate() {
      this.p = this.n;
      this.n = System.currentTimeMillis();

      if (this.p == 0) {
        return;
      }

      if (this.interval == 0) {
        this.interval = this.n - this.p;
        logger.warn("Got heartbeat interval {} ms, from {}.", this.interval, addr);
      }
    }

    void HbCheckUp() throws DiscoveryException {
      if (this.interval == 0) {
        if (this.n == 0) {
          return;
        }
        if (System.currentTimeMillis() - this.n > DefaultInterval * 3) {
          throw new DiscoveryException("heartbeat timeout > 15000ms, from " + addr);
        }
        return;
      }

      if (System.currentTimeMillis() - this.n > this.interval * 3) {
        throw new DiscoveryException("heartbeat timeout > " + (this.interval * 3) + "ms, from " + addr);
      }
    }
  }
}

