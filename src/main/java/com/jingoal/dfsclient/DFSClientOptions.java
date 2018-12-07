package com.jingoal.dfsclient;

import javax.annotation.concurrent.Immutable;

/**
 * Various settings to control the behavior of a <code>DFSClient</code>.
 *
 */
@Immutable
public class DFSClientOptions {

  /**
   * A builder for DFSClientOptions so that DFSClientOptions can be immutable, and to support easier
   * construction through chaining.
   *
   */
  public static class Builder {

    private int socketTimeout = 0;
    private boolean socketKeepAlive = false;

    /**
     * Sets the socket timeout.
     *
     * @param socketTimeout the socket timeout
     * @return {@code this}
     * @see com.jingoal.dfsclient.DFSClientOptions#getSocketTimeout()
     */
    public Builder socketTimeout(final int socketTimeout) {
      if (socketTimeout < 0) {
        throw new IllegalArgumentException("Minimum value is 0");
      }
      this.socketTimeout = socketTimeout;
      return this;
    }

    /**
     * Sets whether socket keep alive is enabled.
     *
     * @param socketKeepAlive keep alive
     * @return {@code this}
     * @see com.jingoal.dfsclient.DFSClientOptions#isSocketKeepAlive()
     */
    public Builder socketKeepAlive(final boolean socketKeepAlive) {
      this.socketKeepAlive = socketKeepAlive;
      return this;
    }

    /**
     * Build an instance of DFSClientOptions.
     *
     * @return the options from this builder
     */
    public DFSClientOptions build() {
      return new DFSClientOptions(this);
    }
  }

  /**
   * Create a new Builder instance. This is a convenience method, equivalent to
   * {@code new DFSClientOptions.Builder()}.
   *
   * @return a new instance of a Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * The socket timeout in milliseconds. It is used for I/O socket read and write operations
   * {@link java.net.Socket#setSoTimeout(int)}
   * <p/>
   * Default is 0 and means no timeout.
   *
   * @return the socket timeout
   */
  public int getSocketTimeout() {
    return socketTimeout;
  }

  /**
   * This flag controls the socket keep alive feature that keeps a connection alive through
   * firewalls {@link java.net.Socket#setKeepAlive(boolean)}
   * <p/>
   * * Default is false.
   *
   * @return whether keep-alive is enabled on each socket
   */
  public boolean isSocketKeepAlive() {
    return socketKeepAlive;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final DFSClientOptions that = (DFSClientOptions) o;
    if (socketTimeout != that.socketTimeout) {
      return false;
    }
    if (socketKeepAlive != that.socketKeepAlive) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = socketTimeout;
    result = 31 * result + (socketKeepAlive ? 1 : 0);
    return result;
  }

  private DFSClientOptions(final Builder builder) {
    socketTimeout = builder.socketTimeout;
    socketKeepAlive = builder.socketKeepAlive;
  }

  private final int socketTimeout;
  private final boolean socketKeepAlive;
}
