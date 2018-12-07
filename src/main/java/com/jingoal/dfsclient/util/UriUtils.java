package com.jingoal.dfsclient.util;

import java.net.InetSocketAddress;

public class UriUtils {
  public static final int defaultPort = 10000;

  public static InetSocketAddress getSocketAddress(String uri) {
    String[] u = uri.split(":");
    if (u.length > 1) {
      return new InetSocketAddress(u[0], Integer.parseInt(u[1]));
    }
    return new InetSocketAddress(u[0], defaultPort);
  }
}
