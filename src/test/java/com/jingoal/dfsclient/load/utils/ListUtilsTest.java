package com.jingoal.dfsclient.load.utils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.load.utils.ListUtils;

public class ListUtilsTest {
  private static final Logger logger = LoggerFactory.getLogger(ListUtilsTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {}

  @AfterClass
  public static void tearDownAfterClass() throws Exception {}

  @Test
  public void testGetAllNodes() {
    List<InetSocketAddress> list1 = new ArrayList<InetSocketAddress>();
    list1.add(new InetSocketAddress("192.168.111.1", 11211));
    list1.add(new InetSocketAddress("192.168.111.2", 11211));

    List<InetSocketAddress> list2 = new ArrayList<InetSocketAddress>();
    list2.add(new InetSocketAddress("192.168.111.2", 11211));
    list2.add(new InetSocketAddress("192.168.111.3", 11211));

    List<InetSocketAddress> list = ListUtils.getAllNodes(list1, list2);
    Assert.assertTrue(list.size() == 3);
    for (InetSocketAddress node : list) {
      logger.debug("All nodes : {}", node);
    }
  }

  @Test
  public void testGetUnChangeNodes() {
    List<InetSocketAddress> list1 = new ArrayList<InetSocketAddress>();
    list1.add(new InetSocketAddress("192.168.111.1", 11211));
    list1.add(new InetSocketAddress("192.168.111.2", 11211));

    List<InetSocketAddress> list2 = new ArrayList<InetSocketAddress>();
    list2.add(new InetSocketAddress("192.168.111.2", 11211));
    list2.add(new InetSocketAddress("192.168.111.3", 11211));

    List<InetSocketAddress> list = ListUtils.getUnChangeNodes(list1, list2);
    Assert.assertTrue(list.size() == 1);
    Assert.assertEquals(list.get(0).getHostName(), "192.168.111.2");
    for (InetSocketAddress node : list) {
      logger.debug("UnChange nodes : {}", node);
    }
  }

  @Test
  public void testGetAddNodes() {
    List<InetSocketAddress> list1 = new ArrayList<InetSocketAddress>();
    list1.add(new InetSocketAddress("192.168.111.1", 11211));
    list1.add(new InetSocketAddress("192.168.111.2", 11211));

    List<InetSocketAddress> list2 = new ArrayList<InetSocketAddress>();
    list2.add(new InetSocketAddress("192.168.111.2", 11211));
    list2.add(new InetSocketAddress("192.168.111.3", 11211));

    List<InetSocketAddress> list = ListUtils.getAddNodes(list1, list2);
    Assert.assertTrue(list.size() == 1);
    Assert.assertEquals(list.get(0).getHostName(), "192.168.111.3");
    for (InetSocketAddress node : list) {
      logger.debug("Add nodes : {}", node);
    }
  }

  @Test
  public void testGetRemoveNodes() {
    List<InetSocketAddress> list1 = new ArrayList<InetSocketAddress>();
    list1.add(new InetSocketAddress("192.168.111.1", 11211));
    list1.add(new InetSocketAddress("192.168.111.2", 11211));

    List<InetSocketAddress> list2 = new ArrayList<InetSocketAddress>();
    list2.add(new InetSocketAddress("192.168.111.2", 11211));
    list2.add(new InetSocketAddress("192.168.111.3", 11211));

    List<InetSocketAddress> list = ListUtils.getRemoveNodes(list1, list2);
    Assert.assertTrue(list.size() == 1);
    Assert.assertEquals(list.get(0).getHostName(), "192.168.111.1");
    for (InetSocketAddress node : list) {
      logger.debug("Remove nodes : {}", node);
    }
  }

  @Test
  public void testGetAddNodes2() {
    List<InetSocketAddress> emptyList = new ArrayList<InetSocketAddress>();

    List<InetSocketAddress> updateList = new ArrayList<InetSocketAddress>();
    updateList.add(new InetSocketAddress("192.168.111.11", 11211));
    updateList.add(new InetSocketAddress("192.168.111.12", 11211));

    List<InetSocketAddress> list = ListUtils.getAddNodes(emptyList, updateList);
    Assert.assertTrue(list.size() == 2);
  }

  @Test
  public void testGetRemoveNodes2() {
    List<InetSocketAddress> emptyList = new ArrayList<InetSocketAddress>();

    List<InetSocketAddress> updateList = new ArrayList<InetSocketAddress>();
    updateList.add(new InetSocketAddress("192.168.111.11", 11211));
    updateList.add(new InetSocketAddress("192.168.111.12", 11211));

    List<InetSocketAddress> list = ListUtils.getRemoveNodes(emptyList, updateList);
    Assert.assertTrue(list.size() == 0);
  }

  @Test
  public void testGetAddNodes3() {
    List<InetSocketAddress> origList = new ArrayList<InetSocketAddress>();
    origList.add(new InetSocketAddress("192.168.111.11", 11211));
    origList.add(new InetSocketAddress("192.168.111.12", 11211));

    List<InetSocketAddress> currList = new ArrayList<InetSocketAddress>();

    List<InetSocketAddress> list = ListUtils.getAddNodes(origList, currList);
    Assert.assertTrue(list.size() == 0);
  }

  @Test
  public void testGetRemoveNodes3() {
    List<InetSocketAddress> origList = new ArrayList<InetSocketAddress>();
    origList.add(new InetSocketAddress("192.168.111.11", 11211));
    origList.add(new InetSocketAddress("192.168.111.12", 11211));

    List<InetSocketAddress> currList = new ArrayList<InetSocketAddress>();

    List<InetSocketAddress> list = ListUtils.getRemoveNodes(origList, currList);
    Assert.assertTrue(list.size() == 2);
  }
}
