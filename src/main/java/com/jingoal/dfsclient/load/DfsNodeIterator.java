package com.jingoal.dfsclient.load;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

import com.jingoal.dfsclient.load.utils.ConsistentHashUtils;
import com.jingoal.dfsclient.load.utils.HashAlgorithm;

/**
 * Implements an Iterator which the DfsNodeLocator may return to a client for iterating through
 * alternate nodes for a given key.
 */
public class DfsNodeIterator implements Iterator<InetSocketAddress> {

  private final String key;
  private long hashVal;
  private int remainingTries;
  private int numTries = 1;
  private final HashAlgorithm hashAlg;
  private final ConcurrentSkipListMap<Long, InetSocketAddress> continuum;

  /**
   *
   * @param key the key to iterate for.
   * @param tryCount the number of tries until giving up.
   * @param continuum the continuum in the form of a TreeMap to be used when selecting a node.
   * @param hashAlg the hash algorithm to use when selecting within the continuumq.
   */
  protected DfsNodeIterator(final String key, final int tryCount,
      final ConcurrentSkipListMap<Long, InetSocketAddress> continuum, final HashAlgorithm hashAlg) {
    this.continuum = continuum;
    this.hashAlg = hashAlg;
    this.key = key;
    hashVal = hashAlg.hash(key);
    remainingTries = tryCount;
  }

  private void nextHash() {
    long tmpKey = hashAlg.hash((numTries++) + key);
    hashVal += (int) (tmpKey ^ (tmpKey >>> 32));
    hashVal &= 0xffffffffL; /* truncate to 32-bits */
    remainingTries--;
  }

  @Override
  public boolean hasNext() {
    return remainingTries > 0;
  }

  @Override
  public InetSocketAddress next() {
    try {
      return ConsistentHashUtils.searchForKey(continuum, hashVal);
    } finally {
      nextHash();
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove not supported.");
  }
}
