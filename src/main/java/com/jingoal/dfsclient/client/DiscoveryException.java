package com.jingoal.dfsclient.client;

public class DiscoveryException extends RuntimeException {
  private static final long serialVersionUID = -2416142089394631467L;

  public DiscoveryException(String message) {
    super(message);
  }

  public DiscoveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
