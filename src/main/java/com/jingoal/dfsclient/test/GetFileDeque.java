package com.jingoal.dfsclient.test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.DFSInputStream;
import com.jingoal.dfsclient.DFSShardClientV20;
import com.jingoal.dfsclient.util.IoMd5Utils;
import com.jingoal.dfsclient.util.IoMd5Utils.StreamData;
import com.jingoal.dfsclient.util.ThreadPoolUtils;

public class GetFileDeque extends WorkDeque<String> {
  private static final Logger logger = LoggerFactory.getLogger(GetFileDeque.class);

  private DFSShardClientV20 client;
  private ExecutorService threadPool;
  private long domain;// 公司ID

  public GetFileDeque(final int batch) {
    super(batch);
  }

  @Override
  protected void process(final Collection<String> list) {
    Collection<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
    for (String fid : list) {
      tasks.add(new GetFileTask(fid));
    }
    try {
      threadPool.invokeAll(tasks);
      tasks.clear();
      tasks = null;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
  }

  public void setClient(final DFSShardClientV20 client) {
    this.client = client;
  }

  public void setThreadPool(final ExecutorService threadPool) {
    this.threadPool = threadPool;
  }

  public void setDomain(final long domain) {
    this.domain = domain;
  }

  class GetFileTask implements Callable<Void> {

    private String fid;

    public GetFileTask(final String fid) {
      this.fid = fid;
    }

    @Override
    public Void call() throws Exception {
      ThreadPoolUtils.process(fid, new ThreadPoolUtils.ThreadProcessor() {

        @Override
        public void process() {
          logger.info("Start get file {}...", fid);
          try {
            DFSInputStream input = client.getInputStream(fid, domain);
            long size = ((Number) input.getMetadata("length")).longValue();
            String md5 = String.valueOf(input.getMetadata("md5"));

            StreamData data = IoMd5Utils.md5(input);
            if (data.getMd5().equals(md5) && data.getSize() == size) {
              logger.info("Get file success, fid:{} domain:{}", fid, domain);
            } else {
              logger.warn("Expect md5:{} and size:{}, but cal md5:{} and size:{}", md5, size,
                  data.getMd5(), data.getSize());
            }
          } catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      });

      return null;
    }
  }
}
