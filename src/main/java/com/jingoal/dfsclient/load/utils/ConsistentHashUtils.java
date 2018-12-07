package com.jingoal.dfsclient.load.utils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.load.NodeLocatorCfg;

public final class ConsistentHashUtils {
  private static final Logger logger = LoggerFactory.getLogger(ConsistentHashUtils.class);

  private ConsistentHashUtils() {}

  /**
   * 初始化一致哈希环.
   *
   * @param nodes 节点集合.
   * @param continuum 一致哈希环，SortedMap实现.
   * @param hashAlg 一致哈希算法.
   * @param config 配置.
   */
  public static <T> void initContinuum(final List<T> nodes,
      final ConcurrentSkipListMap<Long, T> continuum, final HashAlgorithm hashAlg,
      final NodeLocatorCfg<T> config) {
    for (T node : nodes) {
      addToContinuum(node, continuum, hashAlg, config);
    }
  }

  /**
   * 添加一个节点到一致哈希环.
   *
   * @param node 新增节点.
   * @param continuum 一致哈希环，SortedMap实现.
   * @param hashAlg 一致哈希算法.
   * @param config 配置.
   */
  public static <T> void addToContinuum(final T node,
      final ConcurrentSkipListMap<Long, T> continuum, final HashAlgorithm hashAlg,
      final NodeLocatorCfg<T> config) {
    int numReps = config.getNodeRepetitions();

    for (int i = 0; i < numReps / 4; i++) {
      byte[] digest = HashAlgorithm.computeMd5(config.getKeyForNode(node, i));
      for (int h = 0; h < 4; h++) {
        long k = hashAlg.hash(digest, h);
        continuum.put(k, node);
        logger.debug("Adding node {} in position {}", node, k);
      }
    }
  }

  /**
   * 从一致哈希环移除一个节点.
   *
   * @param node 移除节点.
   * @param continuum 一致哈希环，SortedMap实现.
   * @param hashAlg 一致哈希算法.
   * @param config 配置.
   */
  public static <T> void removeFromContinuum(final T node,
      final ConcurrentSkipListMap<Long, T> continuum, final HashAlgorithm hashAlg,
      final NodeLocatorCfg<T> config) {
    int numReps = config.getNodeRepetitions();

    for (int i = 0; i < numReps / 4; i++) {
      byte[] digest = HashAlgorithm.computeMd5(config.getKeyForNode(node, i));
      for (int h = 0; h < 4; h++) {
        long k = hashAlg.hash(digest, h);
        continuum.remove(k);
        logger.debug("Remove node {} in position {}", node, k);
      }
    }
  }

  /**
   * 根据传入的哈希值查找对应节点.
   *
   * @param continuum 一致哈希环，SortedMap实现.
   * @param hash 哈希值.
   * @return 对应节点.
   */
  public static <T> T searchForKey(final ConcurrentSkipListMap<Long, T> continuum,
      final long hash) {
    final T rv;
    Long key = hash;
    if (!continuum.containsKey(key)) {
      key = continuum.ceilingKey(key);
      if (key == null) {
        key = continuum.firstKey();
      }
    }
    rv = continuum.get(key);
    return rv;
  }
}
