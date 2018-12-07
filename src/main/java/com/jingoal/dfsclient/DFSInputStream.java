package com.jingoal.dfsclient;

import java.io.InputStream;

/**
 * DFS版的InputStream
 *
 * @author yhhan
 *
 */
public abstract class DFSInputStream extends InputStream {
  /**
   * getMetadata returns metadata by given key.
   *
   * @param key
   * @return
   */
  public abstract Object getMetadata(String key);
}
