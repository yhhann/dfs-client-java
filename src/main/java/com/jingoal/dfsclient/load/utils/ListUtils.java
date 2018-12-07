package com.jingoal.dfsclient.load.utils;

import java.util.ArrayList;
import java.util.List;

public final class ListUtils {

  private ListUtils() {}

  /**
   * 获取无重复并集,返回一个新集合.
   */
  public static <T> List<T> getAllNodes(final List<T> orig, final List<T> curr) {
    List<T> list = new ArrayList<T>(orig);
    list.removeAll(curr);
    list.addAll(curr);
    return list;
  }

  /**
   * 获取交集,返回一个新集合.
   *
   * @param orig
   * @param curr
   * @return
   */
  public static <T> List<T> getUnChangeNodes(final List<T> orig, final List<T> curr) {
    List<T> list = new ArrayList<T>(orig);
    list.retainAll(curr);
    return list;
  }

  /**
   * 获取新增节点集合,返回一个新集合.
   *
   * @param orig
   * @param curr
   * @return
   */
  public static <T> List<T> getAddNodes(final List<T> orig, final List<T> curr) {
    List<T> list = new ArrayList<T>(curr);
    list.removeAll(orig);
    return list;
  }

  /**
   * 获取删除节点集合,返回一个新集合.
   *
   * @param orig
   * @param curr
   * @return
   */
  public static <T> List<T> getRemoveNodes(final List<T> orig, final List<T> curr) {
    List<T> list = new ArrayList<T>(orig);
    list.removeAll(curr);
    return list;
  }
}
