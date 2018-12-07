package com.jingoal.dfsclient;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.cache.DiskCache;
import com.jingoal.dfsclient.cache.LocalDiskCache;
import com.jingoal.dfsclient.cache.PingPangCacheAction;
import com.jingoal.dfsclient.cache.SwitchCallback;
import com.jingoal.dfsclient.client.ServerListener;
import com.jingoal.dfsclient.client.ServerMonitor;
import com.jingoal.dfsclient.discovery.DfsServer;
import com.jingoal.dfsclient.load.DfsNodeLocator;
import com.jingoal.dfsclient.load.NotInitializedException;
import com.jingoal.dfsclient.metrics.PrometheusProvider;
import com.jingoal.dfsclient.transfer.FileInfo;
import com.jingoal.dfsclient.util.DfsConstants;
import com.jingoal.dfsclient.util.TimerUtils;
import com.jingoal.dfsclient.util.UriUtils;

/**
 * DFS-Client entry program.
 *
 * Add file disk cache, the primary cache directory needs to place the deadbeaf.deadbeaf file, the
 * file content is “deadbeaf“.
 *
 */
public class DFSClientImplV20 implements DFSShardClientV20 {
  private static final Logger logger = LoggerFactory.getLogger(DFSClientImplV20.class);

  private DiskCache diskCache;

  /**
   *
   * Load balance for DFS server nodes.If the nodes change, will trigger the ServerListener method
   * onChange, update the consistent hash continuum.
   *
   * DiskCache will support file local cache.
   *
   * <p>
   * Seeds describes the DFS server hosts and options. The format of the seeds is:
   *
   * <pre>
   *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
   * </pre>
   * <ul>
   * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
   * connection format.</li>
   * <li>{@code host1} is the only required part of the URI. It identifies a server address to
   * connect to.</li>
   * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
   * <li>{@code ?options} are connection options. Note that there is a {@code /} required between
   * the last host and the {@code ?} introducing the options. Options are name=value pairs and the
   * pairs are separated by "&amp;".</li>
   * </ul>
   *
   * <pre>
   * dfslb://192.168.1.1:10000,192.168.1.2:10000
   * dfslb://192.168.1.1,192.168.1.2:10000
   * dfslb://192.168.1.1:10000,192.168.1.2:10000/?k=v
   * dfslb://192.168.1.1,192.168.1.2/?k=v
   * dfslb://192.168.1.1,192.168.1.2:10000/?k=v
   * </pre>
   *
   * @param seeds DFS server seeds.
   * @param clientId The client unique ID.
   * @param diskCache DiskCache.
   */
  public DFSClientImplV20(final String seeds, final String clientId, final DiskCache diskCache) {
    this(seeds, clientId);
    if (diskCache == null) {
      throw new RuntimeException("Please set disk cache first.");
    }
    this.diskCache = diskCache;
  }

  /**
   *
   * Load balance for DFS server nodes.If the nodes change, will trigger the ServerListener method
   * onChange, update the consistent hash continuum.
   *
   * DiskCache of primary and secondary will support file local cache.
   *
   * <p>
   * Seeds describes the DFS server hosts and options. The format of the seeds is:
   *
   * <pre>
   *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
   * </pre>
   * <ul>
   * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
   * connection format.</li>
   * <li>{@code host1} is the only required part of the URI. It identifies a server address to
   * connect to.</li>
   * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
   * <li>{@code ?options} are connection options. Note that there is a {@code /} required between
   * the last host and the {@code ?} introducing the options. Options are name=value pairs and the
   * pairs are separated by "&amp;".</li>
   * </ul>
   *
   * <pre>
   * dfslb://192.168.1.1:10000,192.168.1.2:10000
   * dfslb://192.168.1.1,192.168.1.2:10000
   * dfslb://192.168.1.1:10000,192.168.1.2:10000/?k=v
   * dfslb://192.168.1.1,192.168.1.2/?k=v
   * dfslb://192.168.1.1,192.168.1.2:10000/?k=v
   * </pre>
   *
   * @param seeds DFS server seeds.
   * @param clientId The client unique ID.
   * @param primaryCache DiskCache of primary.
   * @param secondaryCache DiskCache of secondary.
   */
  public DFSClientImplV20(final String seeds, final String clientId, final DiskCache primaryCache,
      final DiskCache secondaryCache) {
    this(seeds, clientId, primaryCache, secondaryCache, null);
  }

  /**
   * Load balance for DFS server nodes.If the nodes change, will trigger the ServerListener method
   * onChange, update the consistent hash continuum.
   *
   * DiskCache of primary and secondary will support file local cache, and PingPangCacheAction
   * support the chance for handle the event of switch cache.
   *
   * <p>
   * Seeds describes the DFS server hosts and options. The format of the seeds is:
   *
   * <pre>
   *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
   * </pre>
   * <ul>
   * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
   * connection format.</li>
   * <li>{@code host1} is the only required part of the URI. It identifies a server address to
   * connect to.</li>
   * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
   * <li>{@code ?options} are connection options. Note that there is a {@code /} required between
   * the last host and the {@code ?} introducing the options. Options are name=value pairs and the
   * pairs are separated by "&amp;".</li>
   * </ul>
   *
   * <pre>
   * dfslb://192.168.1.1:10000,192.168.1.2:10000
   * dfslb://192.168.1.1,192.168.1.2:10000
   * dfslb://192.168.1.1:10000,192.168.1.2:10000/?k=v
   * dfslb://192.168.1.1,192.168.1.2/?k=v
   * dfslb://192.168.1.1,192.168.1.2:10000/?k=v
   * </pre>
   *
   * @param seeds DFS server seeds.
   * @param clientId The client unique ID.
   * @param primaryCache DiskCache of primary.
   * @param secondaryCache DiskCache of secondary.
   * @param cacheAction Handle the event of switch cache.
   */
  public DFSClientImplV20(final String seeds, final String clientId, final DiskCache primaryCache,
      final DiskCache secondaryCache, final PingPangCacheAction cacheAction) {
    this(seeds, clientId);
    if (primaryCache == null || secondaryCache == null) {
      throw new RuntimeException("Please set disk cache first.");
    }
    DiskCache pingCache = primaryCache;
    DiskCache pangCache = secondaryCache;
    diskCache = pingCache;
    if (pingCache instanceof LocalDiskCache) {
      LocalDiskCache localCache = (LocalDiskCache) pingCache;
      localCache.setSwitchCallback(new SwitchCallback() {

        @Override
        public synchronized void onSwitch(final long pingpang) {
          boolean pingOK = (pingpang % 2 == 0);
          long t = pingpang / 1000;

          if (!pingOK && diskCache == pingCache) {
            diskCache = pangCache;
            PrometheusProvider.cacheSpinCounter.inc();
            if (cacheAction != null) {
              try {
                cacheAction.pang();
              } catch (Throwable e) {
                logger.warn("cacheAction.pang() error!");
              }
            }
          } else if (pingOK && diskCache == pangCache) {
            diskCache = pingCache;
            PrometheusProvider.cacheSpinCounter.inc();
            if (cacheAction != null) {
              try {
                cacheAction.ping();
              } catch (Throwable e) {
                logger.warn("cacheAction.ping() error!");
              }
            }
          }
          if (!pingOK) {
            logger.warn("Primary cache ok:" + pingOK + " tm:" + t + " current cache:"
                + ((LocalDiskCache) diskCache).getUuid());
          } else {
            logger.debug("Primary cache ok:" + pingOK + " tm:" + t + " current cache:"
                + ((LocalDiskCache) diskCache).getUuid());
          }

        }
      });
    }
  }

  /**
   * Load balance for DFS server nodes.If the nodes change, will trigger the ServerListener method
   * onChange, update the consistent hash continuum.
   *
   * <p>
   * Seeds describes the DFS server hosts and options. The format of the seeds is:
   *
   * <pre>
   *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
   * </pre>
   * <ul>
   * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
   * connection format.</li>
   * <li>{@code host1} is the only required part of the URI. It identifies a server address to
   * connect to.</li>
   * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
   * <li>{@code ?options} are connection options. Note that there is a {@code /} required between
   * the last host and the {@code ?} introducing the options. Options are name=value pairs and the
   * pairs are separated by "&amp;".</li>
   * </ul>
   *
   * <pre>
   * dfslb://192.168.1.1:10000,192.168.1.2:10000
   * dfslb://192.168.1.1,192.168.1.2:10000
   * dfslb://192.168.1.1:10000,192.168.1.2:10000/?k=v
   * dfslb://192.168.1.1,192.168.1.2/?k=v
   * dfslb://192.168.1.1,192.168.1.2:10000/?k=v
   * </pre>
   *
   * @param seeds DFS server seeds.
   * @param clientId The client unique ID.
   */
  private DFSClientImplV20(final String seeds, final String clientId) {
    try {
      List<InetSocketAddress> seedList = new DFSClientURI(seeds).getSeeds();
      ServerMonitor monitor = new ServerMonitor(seedList, clientId);
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

          loadContinuum(level1, level0);
        }
      });
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Load server will retry when no server is ready.
   *
   * @param level0
   */
  private void loadContinuum(final ConcurrentMap<Long, InetSocketAddress> level1,
      final List<InetSocketAddress> level0) {
    boolean success = false;
    while (!success) {
      try {
        TimerUtils updateLevel1Timer = new TimerUtils();
        try {
          DfsNodeLocator.INSTANCE.updateLevel1(level1);
        } finally {
          PrometheusProvider.updateServerListElapse.labels(DfsConstants.METRICS_UPDATELEVEL1)
              .set(updateLevel1Timer.elapsedMillis());
        }
        TimerUtils updateLevel0Timer = new TimerUtils();
        try {
          DfsNodeLocator.INSTANCE.updateLevel0(level0);
        } finally {
          PrometheusProvider.updateServerListElapse.labels(DfsConstants.METRICS_UPDATELEVEL0)
              .set(updateLevel0Timer.elapsedMillis());
        }
        success = true;
      } catch (Exception e) {
        success = false;
        logger.warn("Can't load handler, please check servers state.");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
        }
      }
    }
  }

  @Override
  public DFSInputStream getInputStream(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, fid);
    return handler.getInputStream(fid, domain);
  }

  @Override
  public DFSOutputStream getOutputStream(final long domain, final String bizname,
      final String filename, final String userid) throws IOException, InvalidArgumentException {
    return getOutputStream(domain, bizname, filename, userid, -1);
  }

  @Override
  public DFSOutputStream getOutputStream(final long domain, final String bizname,
      final String filename, final String userid, final long size)
      throws IOException, InvalidArgumentException {
    String fid = ObjectId.get().toHexString();
    DfsHandler handler = getHandler(domain, fid);
    return handler.getOutputStream(fid, domain, bizname, filename, userid, size);
  }

  @Override
  public DFSDataSource getDataSource(final long domain, final String bizname, final String fid,
      final String userid) throws IOException, FileNotFoundException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, fid);
    return handler.getDataSource(domain, bizname, fid, userid);
  }

  @Override
  public boolean delete(final String fid, final long domain) throws InvalidArgumentException {
    try {
      DfsHandler handler = getHandler(domain, fid);
      return handler.delete(fid, domain);
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public String duplicate(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, fid);
    return handler.duplicate(fid, domain);
  }

  @Override
  public String copy(final long dstdomain, final String bizname, final long srcdomain,
      final String srcFid, final String dstUserid)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    DfsHandler handler = getHandler(dstdomain, srcFid);
    return handler.copy(dstdomain, bizname, srcdomain, srcFid, dstUserid);
  }

  @Override
  public String getByMd5(final long domain, final String md5, final long size)
      throws IOException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, md5);
    return handler.getByMd5(domain, md5, size);
  }

  @Override
  public boolean existByMd5(final long domain, final String md5, final long size)
      throws IOException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, md5);
    return handler.existByMd5(domain, md5, size);
  }

  @Override
  public boolean exist(final String fid, final long domain)
      throws IOException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, fid);
    return handler.exist(fid, domain);
  }

  @Override
  public File getFile(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, fid);
    return handler.getFile(fid, domain);
  }

  @Override
  public FileInfo getFileInfo(final String fid, final long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, fid);
    return handler.getFileInfo(fid, domain);
  }

  @Override
  public String putFile(final long domain, final String bizname, final String filename,
      final File file, final String userid) throws IOException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, file.getName());
    return handler.putFile(domain, bizname, filename, file, userid);
  }

  @Override
  public String putFileWithoutMove(final long domain, final String bizname, final String filename,
      final File file, final String userid) throws IOException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, file.getName());
    return handler.putFileWithoutMove(domain, bizname, filename, file, userid);
  }

  @Override
  public String putFileNotIntoCache(final long domain, final String bizname, final String filename,
      final File file, final String userid) throws IOException, InvalidArgumentException {
    DfsHandler handler = getHandler(domain, file.getName());
    return handler.putFileNotIntoCache(domain, bizname, filename, file, userid);
  }

  /**
   * When method is called, will be based on the domain to obtain the corresponding handler, to
   * achieve the purpose of load balancing
   */
  private DfsHandler getHandler(long domain, String key) throws IOException {
    DfsHandler handler = null;
    try {
      handler = DfsNodeLocator.INSTANCE.getHandler(domain, key);
    } catch (NotInitializedException e) {
      logger.error(e.getMessage());
      throw new IOException(e);
    }
    if (handler != null) {
      handler.setDiskCache(diskCache);
      PrometheusProvider.accessCounter.inc();
    } else {
      logger.warn("Can't find handler for domain {}", domain);
      PrometheusProvider.loseHandlerCounter.inc();
      throw new IOException("Can't find handler for domain " + domain);
    }
    return handler;
  }
}
