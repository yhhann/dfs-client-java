package com.jingoal.dfsclient.load.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Utilities for processing key values.
 */
public final class KeyUtil {

  private KeyUtil() {}

  /**
   * Get the bytes for a key.
   *
   * @param key the key.
   * @return the bytes.
   */
  public static byte[] getKeyBytes(final String key) {
    try {
      return key.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unknown string :" + key, e);
    }
  }

  /**
   * Get the keys in byte form for all of the string keys.
   *
   * @param keys a collection of key.
   * @return return a collection of the byte representations of keys.
   */
  public static Collection<byte[]> getKeyBytes(final Collection<String> keys) {
    Collection<byte[]> rv = new ArrayList<byte[]>(keys.size());
    for (String key : keys) {
      rv.add(getKeyBytes(key));
    }
    return rv;
  }
}
