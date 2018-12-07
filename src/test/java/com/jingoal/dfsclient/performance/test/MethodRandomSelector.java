package com.jingoal.dfsclient.performance.test;

import java.util.Random;

public enum MethodRandomSelector {
  DUPLICATE("duplicate", 1),
  COPY("copy", 2),
  EXISTBYMD5("existByMd5", 3),
  GETBYMD5("getByMd5",4),
  EXIST("exist", 5),
  GETFILEINFO("getFileInfo", 6),
  GETFILE("getFile", 7);

  private static Random random = new Random();
  private int index;
  private String name;

  private MethodRandomSelector(String name, int index) {
    this.index = index;
    this.name = name;
  }

  public int getIndex() {
    return index;
  }

  public String getName() {
    return name;
  }

  public static MethodRandomSelector getMethod() {
    int indx = random.nextInt(7) + 1;
    for (MethodRandomSelector m : MethodRandomSelector.values()) {
      if (m.getIndex() == indx) {
        return m;
      }
    }
    return null;
  }
}
