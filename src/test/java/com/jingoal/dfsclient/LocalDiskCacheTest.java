package com.jingoal.dfsclient;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.jingoal.dfsclient.cache.LocalDiskCache;
import com.jingoal.dfsclient.cache.WriteCallback;
import com.jingoal.dfsclient.util.FileUtils;

public class LocalDiskCacheTest extends LocalDiskCache {
  private static String basedir = System.getProperty("java.io.tmpdir");
  private static long listenInterval = 2L;
  private LocalDiskCache cache;

  public LocalDiskCacheTest(){
    super(basedir, listenInterval);
  }

  @Before
  public void setUp() throws Exception {
    cache = new LocalDiskCacheTest();
  }

  private String fileCachePath="";
  private long domain = 100L;
  @Test
  public void test() {
    String uploadFilePath = System.getProperty("user.dir") + File.separator + "aa.txt";
    File file = new File(uploadFilePath);

    if(!file.exists()){
      FileWriter fw = null;
      try {
        file.createNewFile();
        fw = new FileWriter(file);
        fw.write("I am tester!");
        fw.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }finally {
       try {
        fw.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      }
    }
    try {
      cache.putFileWithoutMove(file, new WriteCallback() {
        @Override
        public String readFrom(final InputStream inputStream)
            throws IOException, InvalidArgumentException {
          String fId = "88888888";
          File cachedFile = FileUtils.getAndEnsureFile4Write(basedir,
              FileUtils.PATH_LEVEL4, domain, fId,2);
          cachedFile.createNewFile();
        //To generate an IOException,set to read-only.
          System.out.println(cachedFile.setReadOnly());
          fileCachePath = cachedFile.getPath();
          return fId;
        }

        @Override
        public long getDomain() {
          return domain;
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidArgumentException e) {
      e.printStackTrace();
    }

    File cacheFile = new File(fileCachePath);
    assertNotNull(fileCachePath);
    assertTrue(cacheFile.length() > 0);
  }

  @Override
  protected void tryWriteCache(File file, String fid, long domain, File cachedFile)
      throws IOException {
  //In order to go through the whole process, you need to turn off the switch.
    cachedFile.setWritable(true);
    super.tryWriteCache(file, fid, domain, cachedFile);
  }

}
