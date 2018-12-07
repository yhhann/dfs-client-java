package com.jingoal.dfsclient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.util.FileUtils;
import com.jingoal.dfsclient.util.IoMd5Utils;

public class DfsGetMd5Test extends AbstractDfsTest {
  private static final Logger logger = LoggerFactory.getLogger(DfsGetMd5Test.class);

  private long domain = 1;
  private int length = 4;// 生成文件大小

  @Test
  public void TestGetByMd5() throws IOException {
    logger.info("Execute begin...");
    try {
      File file = makeFile();
      String md5 = IoMd5Utils.md5(file);
      long size = file.length();
      logger.info(getFileInfo(file));
      String fid = getClient().putFile(domain, "dfs-client", file.getName(), file, "10010");
      logger.info("Successed to putFile, return fid:{}", fid);
      List<String> list = new ArrayList<String>();
      for (int i = 0; i < 5; i++) {
        String rid = getClient().getByMd5(domain, md5, size);
        logger.info("Call getByMd5, return dupId:{}", rid);
        list.add(rid);
      }
      for (String rr : list) {
        doRemove(rr, domain);
      }
      doRemove(fid, domain);
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    } finally {
      logger.info("Execute complete.");
    }
  }

  private File makeFile() throws Exception {
    File file = new File(System.getProperty("java.io.tmpdir"), "/tmp.txt");
    Random rand = new Random();
    byte[] byteArray = new byte[rand.nextInt(length)];
    OutputStream output = new FileOutputStream(file);
    FileUtils.copy(new ByteArrayInputStream(byteArray), output);// will close stream
    return file;
  }
}
