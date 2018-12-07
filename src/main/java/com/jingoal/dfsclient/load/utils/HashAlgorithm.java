package com.jingoal.dfsclient.load.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Ketama is an implementation of a consistent hashing algorithm. This algorithm refer from
 * net.spy.spymemcached,version 2.12.0.
 *
 */
public enum HashAlgorithm {
  /**
   * MD5-based hash algorithm used by ketama.
   */
  KETAMA_HASH;

  public long hash(final byte[] digest, final int nTime) {
    long rv = ((long) (digest[3 + nTime * 4] & 0xFF) << 24)
        | ((long) (digest[2 + nTime * 4] & 0xFF) << 16)
        | ((long) (digest[1 + nTime * 4] & 0xFF) << 8) | (digest[0 + nTime * 4] & 0xFF);
    return rv & 0xffffffffL; /* Truncate to 32-bits */
  }

  /**
   * Compute the hash for the given key.
   *
   * @return a positive integer hash.
   */
  public long hash(final String key) {
    byte[] digest = HashAlgorithm.computeMd5(key);
    return hash(digest, 0);
  }

  /**
   * Get the md5 of the given key.
   */
  public static byte[] computeMd5(final String k) {
    MessageDigest md5;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 not supported", e);
    }
    md5.reset();
    md5.update(KeyUtil.getKeyBytes(k));
    return md5.digest();
  }
}
