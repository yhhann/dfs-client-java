package com.jingoal.dfsclient.cache;

import java.io.IOException;
import java.io.OutputStream;

import com.jingoal.dfsclient.FileNotFoundException;
import com.jingoal.dfsclient.InvalidArgumentException;

/**
 * To carry out the file download operation.
 *
 */
public interface ReadCallback {
  /**
   * Get InputStream from the server, and write to the specified OutputStream.
   *
   * @param output
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public void writeTo(OutputStream output)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * Get the corporation ID.
   *
   * @return
   */
  public long getDomain();
}
