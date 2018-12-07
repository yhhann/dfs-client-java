package com.jingoal.dfsclient.cache;

/**
 *
 * Implemented by DFSClient users, when the cache switch, some additional operations are performed.
 */
public interface PingPangCacheAction {
  /**
   * Switch to primary cache.
   */
  public void ping();

  /**
   * Switch to secondary cache.
   */
  public void pang();
}
