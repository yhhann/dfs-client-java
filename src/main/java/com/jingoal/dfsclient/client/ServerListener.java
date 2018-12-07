package com.jingoal.dfsclient.client;

import java.util.List;

import com.jingoal.dfsclient.discovery.DfsServer;

public interface ServerListener {
  public void onChange(List<DfsServer> servers);
}
