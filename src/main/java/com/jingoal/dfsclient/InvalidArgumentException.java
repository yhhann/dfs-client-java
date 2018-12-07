package com.jingoal.dfsclient;

public class InvalidArgumentException extends Exception {
  private static final long serialVersionUID = 1287571449956345125L;

  public InvalidArgumentException() {
    super();
  }

  public InvalidArgumentException(final String message) {
    super(message);
  }

  public InvalidArgumentException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public InvalidArgumentException(final Throwable cause) {
    super(cause);
  }
}