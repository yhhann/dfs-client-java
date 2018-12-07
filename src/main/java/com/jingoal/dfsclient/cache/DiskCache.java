package com.jingoal.dfsclient.cache;

import java.io.File;
import java.io.IOException;

import com.jingoal.dfsclient.FileNotFoundException;
import com.jingoal.dfsclient.InvalidArgumentException;

/**
 * File processing, such as upload or download functions, provide the local cache interface.
 *
 */
public interface DiskCache {
  /**
   * Download file. If the file exists in the cache, it is returned directly; otherwise, it is
   * download from the server to the cache and return.
   *
   * @param fid The file id.
   * @param callback Download from the server.
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public File getFile(String fid, ReadCallback callback)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * Upload file, and store file into the cache.
   *
   * @param file The upload file.
   * @param callback Upload file to the server.
   * @return
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public String putFile(File file, WriteCallback callback)
      throws IOException, InvalidArgumentException;

  /**
   * Upload file, and store file into the cache.
   *
   * @param file The upload file.
   * @param callback Upload file to the server.
   * @return
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public String putFileWithoutMove(File file, WriteCallback callback)
      throws IOException, InvalidArgumentException;

  /**
   * Delete file from cache.
   *
   * @param fid The file id.
   * @param domain The corporation id.
   * @return
   */
  public boolean deleteCacheFile(String fid, long domain);
}
