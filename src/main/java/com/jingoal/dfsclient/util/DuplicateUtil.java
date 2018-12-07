package com.jingoal.dfsclient.util;

/**
 * Duplicate文件的id操作工具类
 * 
 * @author yhhan
 *
 */
public class DuplicateUtil {
  public static String prefix = "_";

  public static boolean isDuplicateId(String id) {
    if (id.startsWith(prefix)) {
      return true;
    }
    return false;
  }

  public static String getDuplicateId(String realId) {
    if (!isDuplicateId(realId)) {
      return prefix + realId;
    }
    return realId;
  }

  public static String getRealId(String duplicateId) {
    if (isDuplicateId(duplicateId)) {
      return duplicateId.substring(prefix.length());
    } else {
      return duplicateId;
    }
  }
}
