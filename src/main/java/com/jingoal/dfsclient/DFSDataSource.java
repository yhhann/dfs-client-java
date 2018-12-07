package com.jingoal.dfsclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * DFS版的DataSource,注意:只实现了Input,不能使用Output
 *
 * @author yhhan
 *
 */
public class DFSDataSource implements DataSource {
  private String contentType = "application/octet-stream";
  private DFSInputStream in;
  private String fid;

  public DFSDataSource(final DFSInputStream in, final String fid) {
    this.in = in;
    this.fid = fid;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  public void setContentType(final String contentType) {
    this.contentType = contentType;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return in;
  }

  @Override
  public String getName() {
    return fid;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException("Not supported.");
  }
}
