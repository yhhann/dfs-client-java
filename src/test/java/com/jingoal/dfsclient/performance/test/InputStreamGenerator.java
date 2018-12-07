package com.jingoal.dfsclient.performance.test;

import java.io.ByteArrayInputStream;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputStreamGenerator {
  private static final Logger logger = LoggerFactory.getLogger(InputStreamGenerator.class);
  private static byte[] bytes;
  private static Random rd = new Random();

  public static void init(int allocation) {
    long startTime = System.currentTimeMillis();
    logger.info("Start init 'InputStreamGenerator' instance.");
    bytes = new byte[allocation];
    for (int i = 0; i < bytes.length; i++) {
      bytes[i] = (byte) rd.nextInt(127);
    }
    long useTime = System.currentTimeMillis() - startTime;
    logger.info("Init 'InputStreamGenerator' instance complete, size is {} bytes,"
        + " use {} milliseconds.", allocation, useTime);
  }

  public static ByteArrayInputStream getFileStream(int fileLength) {
    bytes[rd.nextInt(fileLength)] = (byte) rd.nextInt(127);
    return new ByteArrayInputStream(bytes, 0, fileLength);
  }

}
