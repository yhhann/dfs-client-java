package com.jingoal.dfsclient.load;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.DfsHandler;
import com.jingoal.dfsclient.DfsHandlerImpl;
import com.jingoal.dfsclient.load.utils.ConsistentHashUtils;
import com.jingoal.dfsclient.load.utils.HashAlgorithm;
import com.jingoal.dfsclient.load.utils.ListUtils;

/**
 * Locating a node by hash value,<br>
 * must call updateNodes method first to load the consistent hash continuum.
 */
public enum DfsNodeLocator {
  INSTANCE;

  private static final Logger logger = LoggerFactory.getLogger(DfsNodeLocator.class);

  private static AtomicBoolean initialized = new AtomicBoolean(false);

  private final ReentrantLock lock = new ReentrantLock();

  private volatile ConcurrentSkipListMap<Long, InetSocketAddress> continuum =
      new ConcurrentSkipListMap<Long, InetSocketAddress>(new LongComparator());
  private volatile CopyOnWriteArrayList<InetSocketAddress> allNodes =
      new CopyOnWriteArrayList<InetSocketAddress>();
  private volatile ConcurrentMap<Long, InetSocketAddress> level1 =
      new ConcurrentHashMap<Long, InetSocketAddress>();
  private volatile ConcurrentMap<InetSocketAddress, DfsHandler> handlers =
      new ConcurrentHashMap<InetSocketAddress, DfsHandler>();

  // The hash algorithm to use when choosing a node in the consistent hash continuum.
  private final HashAlgorithm hashAlg;
  // Node locator configuration.
  private final NodeLocatorCfg<InetSocketAddress> config;

  private DfsNodeLocator() {
    hashAlg = HashAlgorithm.KETAMA_HASH;
    config = new DfsNodeLocatorCfg();
  }

  /**
   * getHandler get a handler according to the input key.
   *
   * @param domain company id.
   * @return
   * @throws NotInitializedException
   */
  public DfsHandler getHandler(final long domain, final String key) throws NotInitializedException {
    DfsHandler handler = null;
    if (level1.containsKey(domain)) {
      handler = getNodeHandler(level1.get(domain));
    }

    if (handler != null) {
      return handler;
    }
    return getHandler(key);
  }

  /**
   * According to the input string hash, locate the corresponding node, and obtain the corresponding
   * handler.Will retry if handler is null.
   *
   * @param key To hash to locate the positioning of the node, should be the company ID, etc..
   * @return
   * @throws NotInitializedException
   */
  private DfsHandler getHandler(final String key) throws NotInitializedException {
    if (!initialized.get()) {
      throw new NotInitializedException("The consistent hash continuum must init first.");
    }

    Iterator<InetSocketAddress> iter = new DfsNodeIterator(key, 7, continuum, hashAlg);
    InetSocketAddress node = null;
    DfsHandler handler = null;
    while (iter.hasNext()) {
      node = iter.next();
      handler = getNodeHandler(node);
      if (handler != null) {
        return handler;
      }
    }

    return null;
  }

  /**
   * deDupl remove the duplicate object from values of map and returns a set.
   *
   * @param servers
   * @return
   */
  private Set<InetSocketAddress> deDupl(final Map<Long, InetSocketAddress> servers) {
    Set<InetSocketAddress> addrSet = new HashSet<InetSocketAddress>();
    for (Map.Entry<Long, InetSocketAddress> e : servers.entrySet()) {
      addrSet.add(e.getValue());
    }

    return addrSet;
  }

  /**
   * Mapping server address to domain collections.
   *
   * @param servers
   * @return
   */
  private Map<InetSocketAddress, List<Long>> filterAddr(
      final Map<Long, InetSocketAddress> servers) {
    Map<InetSocketAddress, List<Long>> addrs = new HashMap<InetSocketAddress, List<Long>>();
    for (Map.Entry<Long, InetSocketAddress> entry : servers.entrySet()) {
      if (!addrs.containsKey(entry.getValue())) {
        List<Long> list = new ArrayList<Long>();
        addrs.put(entry.getValue(), list);
      }
      addrs.get(entry.getValue()).add(entry.getKey());
    }
    return addrs;
  }

  /**
   * Erase the domain mapping when add handler occur exception.
   *
   * @param removed
   */
  private void erased(final List<InetSocketAddress> removed) {
    Map<InetSocketAddress, List<Long>> addrs = this.filterAddr(this.level1);
    for (InetSocketAddress addr : removed) {
      List<Long> domainList = addrs.get(addr);
      for (Long domain : domainList) {
        this.level1.remove(domain);
      }
    }
  }

  /**
   * Update the level1 server map.
   *
   * @param level1 a Map from domain to InetSocketAddress
   * @throws Exception
   */
  public void updateLevel1(final ConcurrentMap<Long, InetSocketAddress> level1) {
    final Lock lock = this.lock;
    lock.lock();
    try {
      for (InetSocketAddress addr : deDupl(this.level1)) {
        if (handlers.containsKey(addr)) {
          DfsHandler handler = handlers.remove(addr);
          try {
            handler.shutdown();
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      }
      this.level1 = level1;
      if (level1.isEmpty()) {
        return;
      }

      List<InetSocketAddress> removed = new ArrayList<InetSocketAddress>();
      for (InetSocketAddress addr : deDupl(level1)) {
        logger.info("Add handler for node {}", addr);
        try {
          handlers.putIfAbsent(addr, new DfsHandlerImpl(addr));
        } catch (Exception e) {
          logger.error("Add handler for node [" + addr + "]  occur exception", e);
          removed.add(addr);
        }
      }
      erased(removed);
    } finally {
      PrintLevel1();
      lock.unlock();
    }
  }

  /**
   * Update the consistent hash continuum with the list of nodes it should update.
   *
   * @param level0 a List of InetSocketAddress for update the consistent hash continuum.
   */
  public void updateLevel0(final List<InetSocketAddress> level0) throws Exception {
    if (level0 == null || level0.size() == 0) {
      cleanContinuum();
      throw new RuntimeException("All servers terminated.");
    }

    final Lock lock = this.lock;
    lock.lock();
    try {
      List<InetSocketAddress> addedNodes = ListUtils.getAddNodes(allNodes, level0);
      List<InetSocketAddress> deletedNodes = ListUtils.getRemoveNodes(allNodes, level0);

      allNodes.clear();
      allNodes.addAll(level0);
      int errCount = 0;

      for (InetSocketAddress node : deletedNodes) {
        removeNodeHandler(node);
      }
      for (InetSocketAddress node : addedNodes) {
        try {
          addNodeHandler(node);
        } catch (Exception e) {
          logger.error("Add handler for node [" + node + "]  occur exception", e);
          errCount++;
          allNodes.remove(node);
        }
      }
      if (errCount == level0.size()) {
        throw new RuntimeException("All servers terminated.");
      }
      logger.info("Current continuum size {}.", allNodes.size());
      initialized.compareAndSet(false, true);
    } finally {
      PrintLevel0();
      lock.unlock();
    }
  }

  private void PrintLevel0() {
    int count = 1;
    StringBuilder sb = new StringBuilder("\nlevel0:\n");
    for (InetSocketAddress addr : allNodes) {
      sb.append("\t").append(count++).append(",\t").append(addr.getHostString()).append(":")
          .append(addr.getPort()).append("\n");
    }
    logger.warn(sb.toString());
  }

  private void PrintLevel1() {
    StringBuilder sb = new StringBuilder("\nlevel1:\n");
    for (Map.Entry<Long, InetSocketAddress> entry : level1.entrySet()) {
      sb.append("\t").append(entry.getKey()).append(":\t").append(entry.getValue().toString())
          .append("\n");
    }
    logger.warn(sb.toString());
  }

  private void cleanContinuum() {
    logger.warn("Clean the consistent hash continuum ,maybe all servers has terminated.");
    final Lock lock = this.lock;
    lock.lock();
    try {
      for (InetSocketAddress node : allNodes) {
        removeNodeHandler(node);
      }
      allNodes.clear();
      continuum.clear();
    } finally {
      lock.unlock();
    }
  }

  private void addNodeHandler(final InetSocketAddress node) throws Exception {
    logger.info("Add handler for node {}", node);
    handlers.putIfAbsent(node, new DfsHandlerImpl(node));
    ConsistentHashUtils.addToContinuum(node, continuum, hashAlg, config);
  }

  private void removeNodeHandler(final InetSocketAddress node) {
    logger.info("Remove handler for node {}", node);
    ConsistentHashUtils.removeFromContinuum(node, continuum, hashAlg, config);
    if (handlers.containsKey(node)) {
      DfsHandler handler = handlers.remove(node);
      try {
        handler.shutdown();
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }

  private DfsHandler getNodeHandler(final InetSocketAddress node) {
    if (handlers.containsKey(node)) {
      return handlers.get(node);
    }
    logger.warn("Failed to get handler for node {}.", node);
    return null;
  }

  class LongComparator implements Comparator<Long> {

    @Override
    public int compare(Long v1, Long v2) {
      if (v1 == v2) {
        return 0;
      }
      return v1 > v2 ? 1 : -1;
    }
  }
}
