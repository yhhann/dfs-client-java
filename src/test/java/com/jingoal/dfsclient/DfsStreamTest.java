package com.jingoal.dfsclient;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.util.FileUtils;
import com.jingoal.dfsclient.util.IOUtils;

public class DfsStreamTest extends AbstractDfsTest {
  private static final Logger logger = LoggerFactory.getLogger(DfsStreamTest.class);

  private static final int EOF = -1;
  private long domain = 1;
  private int length = 4;// 生成文件大小

  @Test
  public void testStream() {
    logger.info("Execute begin...");
    String filename = "dfs-client.txt";
    try {
      byte[] byteArray = new byte[length];
      DFSOutputStream output =
          getClient().getOutputStream(domain, "dfs-client", filename, "10010", length);
      logger.info("Call getOutputStream, filename:{}", filename);
      FileUtils.copy(new ByteArrayInputStream(byteArray), output);// will close stream
      logger.info("File upload complete, fid:{} length:{}", output.getId(), output.getLength());

      DFSInputStream input = null;
      try {
        input = getClient().getInputStream(output.getId(), domain);
        byte[] buffer = new byte[1024 * 4];
        while (EOF != input.read(buffer)) {
        }
        logger.info("Call getInputStream, id:{} name:{} size:{} domain:{} md5:{}",
            input.getMetadata("id"), input.getMetadata("name"), input.getMetadata("size"),
            input.getMetadata("domain"), input.getMetadata("md5"));
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      } finally {
        IOUtils.closeQuietly(input);
      }
      doRemove(output.getId(), domain);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    } finally {
      logger.info("Execute complete.");
    }
  }
}
