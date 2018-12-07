package com.jingoal.dfsclient;

import java.io.File;
import java.net.InetSocketAddress;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.cache.DiskCache;
import com.jingoal.dfsclient.cache.LocalDiskCache;
import com.jingoal.dfsclient.util.IoMd5Utils;

public class AbstractHandlerTest {
  private static final Logger logger = LoggerFactory.getLogger(AbstractHandlerTest.class);

  private static DfsHandler client = null;
  private static String hostname = "192.168.5.239";
  private static int port = 10000;
  private static String cacheDir = "/tmp/data";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    InetSocketAddress seed = new InetSocketAddress(hostname, port);
    try {
      client = new DfsHandlerImpl(seed);

      File dir = new File(cacheDir);
      dir.mkdirs();
      DiskCache diskCache = new LocalDiskCache(cacheDir, 1000 * 10);
      client.setDiskCache(diskCache);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    if (client != null) {
      client.shutdown();
    }
  }

  protected String getFileInfo(final File file) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("file:" + file.getName());
    buffer.append(" size:" + file.length());
    buffer.append(" md5:" + IoMd5Utils.md5(file));
    return buffer.toString();
  }

  public static DfsHandler getClient() {
    return client;
  }
}
