package com.jingoal.dfsclient;

/**
 * 指定的分布式文件没有找到
 *
 */
public class FileNotFoundException extends Exception {
  private static final long serialVersionUID = -8296342312694211014L;

  public FileNotFoundException() {
    super();
  }

  // Remove message prefix, to avoid confusion when packaging IOException.
  public FileNotFoundException(final String message) {
    super(message);
  }

  // Remove message prefix, to avoid confusion when packaging IOException.
  public FileNotFoundException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public FileNotFoundException(final Throwable cause) {
    super(cause);
  }
}
