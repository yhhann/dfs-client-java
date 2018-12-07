package com.jingoal.dfsclient.util;

import org.junit.Test;

/**
 *
 * @author yhhan
 *
 */
public class StackTraceUtilsTest {

  @Test
  public void Test() {
    System.out.println(Level4());
  }

  public String Level4() {
    return Level3();
  }

  public String Level3() {
    return Level2();
  }

  public String Level2() {
    return Level1();
  }

  public String Level1() {
    return Level0();
  }

  public String Level0() {
    return StackTraceUtils.getStackTraceString("StackTraceUtilsTest.Level0", 4);
  }
}
