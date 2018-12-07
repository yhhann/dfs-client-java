package com.jingoal.dfsclient;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.cache.DiskCache;
import com.jingoal.dfsclient.cache.LocalDiskCache;
import com.jingoal.dfsclient.cache.PingPangCacheAction;
import com.jingoal.dfsclient.util.IoMd5Utils;

public class AbstractDfsTest {
  private static final Logger logger = LoggerFactory.getLogger(AbstractDfsTest.class);

  private static DFSShardClientV20 client = null;
  private static String seeds = "192.168.5.239:10000,192.168.5.239:20000";
  private static String pingDir = "/tmp/data1";
  private static String pangDir = "/tmp/data2";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    try {
      DiskCache pingCache = new LocalDiskCache(pingDir, 1000 * 10);
      DiskCache pangCache = new LocalDiskCache(pangDir, 1000 * 10);
      client =
          new DFSClientImplV20(seeds, "10010", pingCache, pangCache, new PingPangCacheAction() {

            @Override
            public void ping() {
              logger.info("ping switch.");
            }

            @Override
            public void pang() {
              logger.info("pang switch.");
            }
          });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {}

  protected void doRemove(final String fid, final long domain) throws Exception {
    boolean isExist = getClient().exist(fid, domain);
    logger.info("Before delete, exist:{} fid:{} domain:{}", isExist, fid, domain);
    boolean delFlag = getClient().delete(fid, domain);
    logger.info("Delete:{} fid:{} domain:{}", delFlag, fid, domain);
    isExist = getClient().exist(fid, domain);
    logger.info("After delete, exist:{} fid:{} domain:{}", isExist, fid, domain);
  }

  protected String getFileInfo(final File file) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("file:" + file.getName());
    buffer.append(" size:" + file.length());
    buffer.append(" md5:" + IoMd5Utils.md5(file));
    return buffer.toString();
  }

  public static DFSShardClientV20 getClient() {
    return client;
  }
}
