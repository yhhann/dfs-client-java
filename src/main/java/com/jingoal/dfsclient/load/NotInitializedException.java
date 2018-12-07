package com.jingoal.dfsclient.load;

public class NotInitializedException extends Exception {
  private static final long serialVersionUID = -6639554376666802260L;

  public NotInitializedException() {
    super();
  }

  public NotInitializedException(final String message) {
    super(message);
  }

  public NotInitializedException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public NotInitializedException(final Throwable cause) {
    super(cause);
  }
}
