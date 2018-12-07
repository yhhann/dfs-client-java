package com.jingoal.dfsclient;

import java.io.OutputStream;

/**
 * DFS版的OutputStream
 *
 * @author yhhan
 *
 */
public abstract class DFSOutputStream extends OutputStream {
  /**
   * Get the fid.
   *
   * @return fid
   */
  public abstract String getId();

  /**
   * 取得文件的长度，必须在调用 close 之后，才能得到准确的文件长度。
   *
   * @return
   */
  public abstract long getLength();
}
