package com.jingoal.dfsclient;

import java.io.IOException;

import com.jingoal.dfsclient.cache.DiskCache;

public interface DfsHandler extends DFSShardClientV20 {
  /**
   * Shutdown the channel. <br>
   * Initiates an orderly shutdown in which preexisting calls continue but new calls are immediately
   * cancelled.
   *
   * @throws InterruptedException
   */
  public void shutdown() throws InterruptedException;

  /**
   * Set the disk cache for file storage.
   *
   * @param diskCache
   */
  public void setDiskCache(final DiskCache diskCache);

  /**
   * 通过指定的参数创建一个分布式文件的输出流.
   *
   * @param fid 文件id, required
   * @param domain 公司ID, required
   * @param bizname 模块名, required
   * @param filename 文件名,可以为null
   * @param userid 用户ID, required
   * @param size File size in byte. When less than 0, will not set timeout.
   * @return
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public DFSOutputStream getOutputStream(String fid, long domain, String bizname, String filename,
      String userid, long size) throws IOException, InvalidArgumentException;
}
