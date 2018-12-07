package com.jingoal.dfsclient.util;

/**
 *
 * @author yhhan
 *
 */
public class StackTraceUtils {

  /**
   * 取得调用的堆栈信息
   *
   * @return
   */
  public static String getStackTraceString() {
    StringBuilder sb = new StringBuilder(Thread.currentThread().getName());
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    for (StackTraceElement st : stackTraceElements) {
      if (st.toString().contains("java.lang.Thread.getStackTrace")
          || st.toString().contains("getStackTraceString")) {
        continue;
      }
      sb.append("\n").append(st);
    }

    return sb.toString();
  }

  /**
   * 取得调用的堆栈信息， 从包含 start 字串的位置开始， 一共打印 level 层。
   *
   * @param start
   * @param level
   * @return
   */
  public static String getStackTraceString(final String start, int level) {
    StringBuilder sb = new StringBuilder("Thread: ").append(Thread.currentThread().getName());
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

    boolean startRecord = false;
    for (StackTraceElement st : stackTraceElements) {
      if (st.toString().contains("java.lang.Thread.getStackTrace")
          || st.toString().contains("getStackTraceString")) {
        continue;
      }

      if (st.toString().contains(start)) {
        startRecord = true;
      }

      if (startRecord) {
        sb.append("\n").append(getConciseDesc(st));
        level--;
      }

      if (level <= 0) {
        break;
      }
    }

    return sb.toString();
  }

  private static String getConciseDesc(final StackTraceElement e) {
    return e.getClassName() + "." + e.getMethodName();
  }
}
