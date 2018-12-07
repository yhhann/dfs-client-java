package com.jingoal.dfsclient.cache;

import java.io.IOException;
import java.io.InputStream;

import com.jingoal.dfsclient.InvalidArgumentException;

/**
 * To carry out the file upload operation.
 *
 */
public interface WriteCallback {
  /**
   * Upload files from the specified InputStream to the server.
   *
   * @param input
   * @return
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public String readFrom(InputStream input) throws IOException, InvalidArgumentException;

  /**
   * Get the corporation ID.
   *
   * @return
   */
  public long getDomain();
}
