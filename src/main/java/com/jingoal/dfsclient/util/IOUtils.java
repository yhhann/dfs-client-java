package com.jingoal.dfsclient.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * General IO stream manipulation utilities.
 * <p>
 * This class provides static utility methods for input/output operations.
 * <ul>
 * <li>closeQuietly - these methods close a stream ignoring nulls and exceptions
 * <li>toXxx/read - these methods read data from a stream
 * <li>write - these methods write data to a stream
 * <li>copy - these methods copy all the data from one stream to another
 * <li>contentEquals - these methods compare the content of two streams
 * </ul>
 * <p>
 * The byte-to-char methods and char-to-byte methods involve a conversion step. Two methods are
 * provided in each case, one that uses the platform default encoding and the other which allows you
 * to specify an encoding. You are encouraged to always specify an encoding because relying on the
 * platform default can lead to unexpected results, for example when moving from development to
 * production.
 * <p>
 * All the methods in this class that read a stream are buffered internally. This means that there
 * is no cause to use a <code>BufferedInputStream</code> or <code>BufferedReader</code>. The default
 * buffer size of 4K has been shown to be efficient in tests.
 * <p>
 * Wherever possible, the methods in this class do <em>not</em> flush or close the stream. This is
 * to avoid making non-portable assumptions about the streams' origin and further use. Thus the
 * caller is still responsible for closing streams after use.
 * <p>
 */
public final class IOUtils {
  // NOTE: This class is focussed on InputStream, OutputStream, Reader and
  // Writer. Each method should take at least one of these as a parameter,
  // or return one of them.

  private static final int EOF = -1;
  /**
   * The Unix directory separator character.
   */
  public static final char DIR_SEPARATOR_UNIX = '/';
  /**
   * The Windows directory separator character.
   */
  public static final char DIR_SEPARATOR_WINDOWS = '\\';
  /**
   * The system directory separator character.
   */
  public static final char DIR_SEPARATOR = File.separatorChar;
  /**
   * The Unix line separator string.
   */
  public static final String LINE_SEPARATOR_UNIX = "\n";
  /**
   * The Windows line separator string.
   */
  public static final String LINE_SEPARATOR_WINDOWS = "\r\n";
  /**
   * The system line separator string.
   */
  public static final String LINE_SEPARATOR;

  static {
    // avoid security issues
    StringWriter buf = new StringWriter();
    PrintWriter out = new PrintWriter(buf);
    out.println();
    LINE_SEPARATOR = buf.toString();
    out.close();
  }

  /**
   * The default buffer size ({@value}) to use for {@link #copyLarge(InputStream, OutputStream)} and
   * {@link #copyLarge(Reader, Writer)}
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  /**
   * The default buffer size to use for the skip() methods.
   */
  private static final int SKIP_BUFFER_SIZE = 2048;

  // Allocated in the relevant skip method if necessary.
  /*
   * N.B. no need to synchronize these because: - we don't care if the buffer is created multiple
   * times (the data is ignored) - we always use the same size buffer, so if it it is recreated it
   * will still be OK (if the buffer size were variable, we would need to synch. to ensure some
   * other thread did not create a smaller one)
   */
  private static char[] SKIP_CHAR_BUFFER;
  private static byte[] SKIP_BYTE_BUFFER;

  /**
   * Instances should NOT be constructed in standard programming.
   */
  private IOUtils() {
    super();
  }

  // -----------------------------------------------------------------------

  /**
   * Closes a URLConnection.
   *
   * @param conn the connection to close.
   */
  public static void close(final URLConnection conn) {
    if (conn instanceof HttpURLConnection) {
      ((HttpURLConnection) conn).disconnect();
    }
  }

  /**
   * Unconditionally close an <code>Reader</code>.
   * <p>
   * Equivalent to {@link Reader#close()}, except any exceptions will be ignored. This is typically
   * used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * char[] data = new char[1024];
   * Reader in = null;
   * try {
   *   in = new FileReader("foo.txt");
   *   in.read(data);
   *   in.close(); // close errors are handled
   * } catch (Exception e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(in);
   * }
   * </pre>
   *
   * @param input the Reader to close, may be null or already closed
   */
  public static void closeQuietly(final Reader input) {
    closeQuietly((Closeable) input);
  }

  /**
   * Unconditionally close a <code>Writer</code>.
   * <p>
   * Equivalent to {@link Writer#close()}, except any exceptions will be ignored. This is typically
   * used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * Writer out = null;
   * try {
   *   out = new StringWriter();
   *   out.write("Hello World");
   *   out.close(); // close errors are handled
   * } catch (Exception e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(out);
   * }
   * </pre>
   *
   * @param output the Writer to close, may be null or already closed
   */
  public static void closeQuietly(final Writer output) {
    closeQuietly((Closeable) output);
  }

  /**
   * Unconditionally close an <code>InputStream</code>.
   * <p>
   * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored. This is
   * typically used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * byte[] data = new byte[1024];
   * InputStream in = null;
   * try {
   *   in = new FileInputStream("foo.txt");
   *   in.read(data);
   *   in.close(); // close errors are handled
   * } catch (Exception e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(in);
   * }
   * </pre>
   *
   * @param input the InputStream to close, may be null or already closed
   */
  public static void closeQuietly(final InputStream input) {
    closeQuietly((Closeable) input);
  }

  /**
   * Unconditionally close an <code>OutputStream</code>.
   * <p>
   * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored. This is
   * typically used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * byte[] data = "Hello, World".getBytes();
   *
   * OutputStream out = null;
   * try {
   *   out = new FileOutputStream("foo.txt");
   *   out.write(data);
   *   out.close(); // close errors are handled
   * } catch (IOException e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(out);
   * }
   * </pre>
   *
   * @param output the OutputStream to close, may be null or already closed
   */
  public static void closeQuietly(final OutputStream output) {
    closeQuietly((Closeable) output);
  }

  /**
   * Unconditionally close a <code>Closeable</code>.
   * <p>
   * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored. This is
   * typically used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * Closeable closeable = null;
   * try {
   *   closeable = new FileReader("foo.txt");
   *   // process closeable
   *   closeable.close();
   * } catch (Exception e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(closeable);
   * }
   * </pre>
   *
   * @param closeable the object to close, may be null or already closed
   */
  public static void closeQuietly(final Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException ioe) {
      // ignore
    }
  }

  /**
   * Unconditionally close a <code>Socket</code>.
   * <p>
   * Equivalent to {@link Socket#close()}, except any exceptions will be ignored. This is typically
   * used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * Socket socket = null;
   * try {
   *   socket = new Socket("http://www.foo.com/", 80);
   *   // process socket
   *   socket.close();
   * } catch (Exception e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(socket);
   * }
   * </pre>
   *
   * @param sock the Socket to close, may be null or already closed
   */
  public static void closeQuietly(final Socket sock) {
    if (sock != null) {
      try {
        sock.close();
      } catch (IOException ioe) {
        // ignored
      }
    }
  }

  /**
   * Unconditionally close a <code>Selector</code>.
   * <p>
   * Equivalent to {@link Selector#close()}, except any exceptions will be ignored. This is
   * typically used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * Selector selector = null;
   * try {
   *   selector = Selector.open();
   *   // process socket
   *
   * } catch (Exception e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(selector);
   * }
   * </pre>
   *
   * @param selector the Selector to close, may be null or already closed
   */
  public static void closeQuietly(final Selector selector) {
    if (selector != null) {
      try {
        selector.close();
      } catch (IOException ioe) {
        // ignored
      }
    }
  }

  /**
   * Unconditionally close a <code>ServerSocket</code>.
   * <p>
   * Equivalent to {@link ServerSocket#close()}, except any exceptions will be ignored. This is
   * typically used in finally blocks.
   * <p>
   * Example code:
   *
   * <pre>
   * ServerSocket socket = null;
   * try {
   *   socket = new ServerSocket();
   *   // process socket
   *   socket.close();
   * } catch (Exception e) {
   *   // error handling
   * } finally {
   *   IOUtils.closeQuietly(socket);
   * }
   * </pre>
   *
   * @param sock the ServerSocket to close, may be null or already closed
   */
  public static void closeQuietly(final ServerSocket sock) {
    if (sock != null) {
      try {
        sock.close();
      } catch (IOException ioe) {
        // ignored
      }
    }
  }

  /**
   * Returns the given reader if it is a {@link BufferedReader}, otherwise creates a
   * toBufferedReader for the given reader.
   *
   * @param reader the reader to wrap or return
   * @return the given reader or a new {@link BufferedReader} for the given reader
   */
  public static BufferedReader toBufferedReader(final Reader reader) {
    return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
  }

  // read toByteArray
  // -----------------------------------------------------------------------
  /**
   * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from
   * @return the requested byte array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(final InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    copy(input, output);
    return output.toByteArray();
  }

  /**
   * Get contents of an <code>InputStream</code> as a <code>byte[]</code>. Use this method instead
   * of <code>toByteArray(InputStream)</code> when <code>InputStream</code> size is known.
   * <b>NOTE:</b> the method checks that the length can safely be cast to an int without truncation
   * before using {@link IOUtils#toByteArray(java.io.InputStream, int)} to read into the byte array.
   * (Arrays can have no more than Integer.MAX_VALUE entries anyway)
   *
   * @param input the <code>InputStream</code> to read from
   * @param size the size of <code>InputStream</code>
   * @return the requested byte array
   * @throws IOException if an I/O error occurs or <code>InputStream</code> size differ from
   *         parameter size
   * @throws IllegalArgumentException if size is less than zero or size is greater than
   *         Integer.MAX_VALUE
   * @see IOUtils#toByteArray(java.io.InputStream, int)
   */
  public static byte[] toByteArray(final InputStream input, final long size) throws IOException {
    if (size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Size cannot be greater than Integer max value: " + size);
    }

    return toByteArray(input, (int) size);
  }

  /**
   * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>. Use this method
   * instead of <code>toByteArray(InputStream)</code> when <code>InputStream</code> size is known
   *
   * @param input the <code>InputStream</code> to read from
   * @param size the size of <code>InputStream</code>
   * @return the requested byte array
   * @throws IOException if an I/O error occurs or <code>InputStream</code> size differ from
   *         parameter size
   * @throws IllegalArgumentException if size is less than zero
   */
  public static byte[] toByteArray(final InputStream input, final int size) throws IOException {
    if (size < 0) {
      throw new IllegalArgumentException("Size must be equal or greater than zero: " + size);
    }

    if (size == 0) {
      return new byte[0];
    }

    byte[] data = new byte[size];
    int offset = 0;
    int readed;

    while (offset < size && (readed = input.read(data, offset, size - offset)) != EOF) {
      offset += readed;
    }

    if (offset != size) {
      throw new IOException("Unexpected readed size. current: " + offset + ", excepted: " + size);
    }

    return data;
  }

  /**
   * Get the contents of a <code>Reader</code> as a <code>byte[]</code> using the default character
   * encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from
   * @return the requested byte array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(final Reader input) throws IOException {
    return toByteArray(input, Charset.defaultCharset());
  }

  /**
   * Get the contents of a <code>Reader</code> as a <code>byte[]</code> using the specified
   * character encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested byte array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static byte[] toByteArray(final Reader input, final Charset encoding) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    copy(input, output, encoding);
    return output.toByteArray();
  }

  /**
   * Get the contents of a <code>Reader</code> as a <code>byte[]</code> using the specified
   * character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested byte array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static byte[] toByteArray(final Reader input, final String encoding) throws IOException {
    return toByteArray(input, toCharset(encoding));
  }

  /**
   * Get the contents of a <code>URI</code> as a <code>byte[]</code>.
   *
   * @param uri the <code>URI</code> to read
   * @return the requested byte array
   * @throws NullPointerException if the uri is null
   * @throws IOException if an I/O exception occurs
   */
  public static byte[] toByteArray(final URI uri) throws IOException {
    return IOUtils.toByteArray(uri.toURL());
  }

  /**
   * Get the contents of a <code>URL</code> as a <code>byte[]</code>.
   *
   * @param url the <code>URL</code> to read
   * @return the requested byte array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O exception occurs
   */
  public static byte[] toByteArray(final URL url) throws IOException {
    URLConnection conn = url.openConnection();
    try {
      return IOUtils.toByteArray(conn);
    } finally {
      close(conn);
    }
  }

  /**
   * Get the contents of a <code>URLConnection</code> as a <code>byte[]</code>.
   *
   * @param urlConn the <code>URLConnection</code> to read
   * @return the requested byte array
   * @throws NullPointerException if the urlConn is null
   * @throws IOException if an I/O exception occurs
   */
  public static byte[] toByteArray(final URLConnection urlConn) throws IOException {
    InputStream inputStream = urlConn.getInputStream();
    try {
      return IOUtils.toByteArray(inputStream);
    } finally {
      inputStream.close();
    }
  }

  // read char[]
  // -----------------------------------------------------------------------
  /**
   * Get the contents of an <code>InputStream</code> as a character array using the default
   * character encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param is the <code>InputStream</code> to read from
   * @return the requested character array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static char[] toCharArray(final InputStream is) throws IOException {
    return toCharArray(is, Charset.defaultCharset());
  }

  /**
   * Get the contents of an <code>InputStream</code> as a character array using the specified
   * character encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param is the <code>InputStream</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested character array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static char[] toCharArray(final InputStream is, final Charset encoding)
      throws IOException {
    CharArrayWriter output = new CharArrayWriter();
    copy(is, output, encoding);
    return output.toCharArray();
  }

  /**
   * Get the contents of an <code>InputStream</code> as a character array using the specified
   * character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param is the <code>InputStream</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested character array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static char[] toCharArray(final InputStream is, final String encoding) throws IOException {
    return toCharArray(is, toCharset(encoding));
  }

  /**
   * Get the contents of a <code>Reader</code> as a character array.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from
   * @return the requested character array
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static char[] toCharArray(final Reader input) throws IOException {
    CharArrayWriter sw = new CharArrayWriter();
    copy(input, sw);
    return sw.toCharArray();
  }

  // read toString
  // -----------------------------------------------------------------------
  /**
   * Get the contents of an <code>InputStream</code> as a String using the default character
   * encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static String toString(final InputStream input) throws IOException {
    return toString(input, Charset.defaultCharset());
  }

  /**
   * Get the contents of an <code>InputStream</code> as a String using the specified character
   * encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * </p>
   *
   * @param input the <code>InputStream</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static String toString(final InputStream input, final Charset encoding)
      throws IOException {
    StringWriter sw = new StringWriter();
    copy(input, sw, encoding);
    return sw.toString();
  }

  /**
   * Get the contents of an <code>InputStream</code> as a String using the specified character
   * encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static String toString(final InputStream input, final String encoding) throws IOException {
    return toString(input, toCharset(encoding));
  }

  /**
   * Get the contents of a <code>Reader</code> as a String.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static String toString(final Reader input) throws IOException {
    StringWriter sw = new StringWriter();
    copy(input, sw);
    return sw.toString();
  }

  /**
   * Gets the contents at the given URI.
   *
   * @param uri The URI source.
   * @return The contents of the URL as a String.
   * @throws IOException if an I/O exception occurs.
   */
  public static String toString(final URI uri) throws IOException {
    return toString(uri, Charset.defaultCharset());
  }

  /**
   * Gets the contents at the given URI.
   *
   * @param uri The URI source.
   * @param encoding The encoding name for the URL contents.
   * @return The contents of the URL as a String.
   * @throws IOException if an I/O exception occurs.
   */
  public static String toString(final URI uri, final Charset encoding) throws IOException {
    return toString(uri.toURL(), encoding);
  }

  /**
   * Gets the contents at the given URI.
   *
   * @param uri The URI source.
   * @param encoding The encoding name for the URL contents.
   * @return The contents of the URL as a String.
   * @throws IOException if an I/O exception occurs.
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static String toString(final URI uri, final String encoding) throws IOException {
    return toString(uri, toCharset(encoding));
  }

  /**
   * Gets the contents at the given URL.
   *
   * @param url The URL source.
   * @return The contents of the URL as a String.
   * @throws IOException if an I/O exception occurs.
   */
  public static String toString(final URL url) throws IOException {
    return toString(url, Charset.defaultCharset());
  }

  /**
   * Gets the contents at the given URL.
   *
   * @param url The URL source.
   * @param encoding The encoding name for the URL contents.
   * @return The contents of the URL as a String.
   * @throws IOException if an I/O exception occurs.
   */
  public static String toString(final URL url, final Charset encoding) throws IOException {
    InputStream inputStream = url.openStream();
    try {
      return toString(inputStream, encoding);
    } finally {
      inputStream.close();
    }
  }

  /**
   * Gets the contents at the given URL.
   *
   * @param url The URL source.
   * @param encoding The encoding name for the URL contents.
   * @return The contents of the URL as a String.
   * @throws IOException if an I/O exception occurs.
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static String toString(final URL url, final String encoding) throws IOException {
    return toString(url, toCharset(encoding));
  }

  /**
   * Get the contents of a <code>byte[]</code> as a String using the specified character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   *
   * @param input the byte array to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs (never occurs)
   */
  public static String toString(final byte[] input, final String encoding) throws IOException {
    return new String(input, toCharset(encoding));
  }

  // readLines
  // -----------------------------------------------------------------------
  /**
   * Get the contents of an <code>InputStream</code> as a list of Strings, one entry per line, using
   * the default character encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from, not null
   * @return the list of Strings, never null
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(final InputStream input) throws IOException {
    return readLines(input, Charset.defaultCharset());
  }

  /**
   * Get the contents of an <code>InputStream</code> as a list of Strings, one entry per line, using
   * the specified character encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from, not null
   * @param encoding the encoding to use, null means platform default
   * @return the list of Strings, never null
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(final InputStream input, final Charset encoding)
      throws IOException {
    InputStreamReader reader = new InputStreamReader(input, encoding);
    return readLines(reader);
  }

  /**
   * Get the contents of an <code>InputStream</code> as a list of Strings, one entry per line, using
   * the specified character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from, not null
   * @param encoding the encoding to use, null means platform default
   * @return the list of Strings, never null
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static List<String> readLines(final InputStream input, final String encoding)
      throws IOException {
    return readLines(input, toCharset(encoding));
  }

  /**
   * Get the contents of a <code>Reader</code> as a list of Strings, one entry per line.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from, not null
   * @return the list of Strings, never null
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static List<String> readLines(final Reader input) throws IOException {
    BufferedReader reader = toBufferedReader(input);
    List<String> list = new ArrayList<String>();
    String line = reader.readLine();
    while (line != null) {
      list.add(line);
      line = reader.readLine();
    }
    return list;
  }

  // -----------------------------------------------------------------------
  /**
   * Convert the specified CharSequence to an input stream, encoded as bytes using the default
   * character encoding of the platform.
   *
   * @param input the CharSequence to convert
   * @return an input stream
   */
  public static InputStream toInputStream(final CharSequence input) {
    return toInputStream(input, Charset.defaultCharset());
  }

  /**
   * Convert the specified CharSequence to an input stream, encoded as bytes using the specified
   * character encoding.
   *
   * @param input the CharSequence to convert
   * @param encoding the encoding to use, null means platform default
   * @return an input stream
   */
  public static InputStream toInputStream(final CharSequence input, final Charset encoding) {
    return toInputStream(input.toString(), encoding);
  }

  /**
   * Convert the specified CharSequence to an input stream, encoded as bytes using the specified
   * character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   *
   * @param input the CharSequence to convert
   * @param encoding the encoding to use, null means platform default
   * @return an input stream
   * @throws IOException if the encoding is invalid
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static InputStream toInputStream(final CharSequence input, final String encoding)
      throws IOException {
    return toInputStream(input, toCharset(encoding));
  }

  // -----------------------------------------------------------------------
  /**
   * Convert the specified string to an input stream, encoded as bytes using the default character
   * encoding of the platform.
   *
   * @param input the string to convert
   * @return an input stream
   */
  public static InputStream toInputStream(final String input) {
    return toInputStream(input, Charset.defaultCharset());
  }

  /**
   * Convert the specified string to an input stream, encoded as bytes using the specified character
   * encoding.
   *
   * @param input the string to convert
   * @param encoding the encoding to use, null means platform default
   * @return an input stream
   */
  public static InputStream toInputStream(final String input, final Charset encoding) {
    return new ByteArrayInputStream(input.getBytes(encoding));
  }

  /**
   * Convert the specified string to an input stream, encoded as bytes using the specified character
   * encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   *
   * @param input the string to convert
   * @param encoding the encoding to use, null means platform default
   * @return an input stream
   * @throws IOException if the encoding is invalid
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static InputStream toInputStream(final String input, final String encoding)
      throws IOException {
    byte[] bytes = input.getBytes(toCharset(encoding));
    return new ByteArrayInputStream(bytes);
  }

  // write byte[]
  // -----------------------------------------------------------------------
  /**
   * Writes bytes from a <code>byte[]</code> to an <code>OutputStream</code>.
   *
   * @param data the byte array to write, do not modify during output, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final byte[] data, final OutputStream output) throws IOException {
    if (data != null) {
      output.write(data);
    }
  }

  /**
   * Writes bytes from a <code>byte[]</code> to chars on a <code>Writer</code> using the default
   * character encoding of the platform.
   * <p>
   * This method uses {@link String#String(byte[])}.
   *
   * @param data the byte array to write, do not modify during output, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final byte[] data, final Writer output) throws IOException {
    write(data, output, Charset.defaultCharset());
  }

  /**
   * Writes bytes from a <code>byte[]</code> to chars on a <code>Writer</code> using the specified
   * character encoding.
   * <p>
   * This method uses {@link String#String(byte[], String)}.
   *
   * @param data the byte array to write, do not modify during output, null ignored
   * @param output the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final byte[] data, final Writer output, final Charset encoding)
      throws IOException {
    if (data != null) {
      output.write(new String(data, encoding));
    }
  }

  /**
   * Writes bytes from a <code>byte[]</code> to chars on a <code>Writer</code> using the specified
   * character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#String(byte[], String)}.
   *
   * @param data the byte array to write, do not modify during output, null ignored
   * @param output the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static void write(final byte[] data, final Writer output, final String encoding)
      throws IOException {
    write(data, output, toCharset(encoding));
  }

  // write char[]
  // -----------------------------------------------------------------------
  /**
   * Writes chars from a <code>char[]</code> to a <code>Writer</code> using the default character
   * encoding of the platform.
   *
   * @param data the char array to write, do not modify during output, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final char[] data, final Writer output) throws IOException {
    if (data != null) {
      output.write(data);
    }
  }

  /**
   * Writes chars from a <code>char[]</code> to bytes on an <code>OutputStream</code>.
   * <p>
   * This method uses {@link String#String(char[])} and {@link String#getBytes()}.
   *
   * @param data the char array to write, do not modify during output, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final char[] data, final OutputStream output) throws IOException {
    write(data, output, Charset.defaultCharset());
  }

  /**
   * Writes chars from a <code>char[]</code> to bytes on an <code>OutputStream</code> using the
   * specified character encoding.
   * <p>
   * This method uses {@link String#String(char[])} and {@link String#getBytes(String)}.
   *
   * @param data the char array to write, do not modify during output, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final char[] data, final OutputStream output, final Charset encoding)
      throws IOException {
    if (data != null) {
      output.write(new String(data).getBytes(encoding));
    }
  }

  /**
   * Writes chars from a <code>char[]</code> to bytes on an <code>OutputStream</code> using the
   * specified character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#String(char[])} and {@link String#getBytes(String)}.
   *
   * @param data the char array to write, do not modify during output, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static void write(final char[] data, final OutputStream output, final String encoding)
      throws IOException {
    write(data, output, toCharset(encoding));
  }

  // write CharSequence
  // -----------------------------------------------------------------------
  /**
   * Writes chars from a <code>CharSequence</code> to a <code>Writer</code>.
   *
   * @param data the <code>CharSequence</code> to write, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final CharSequence data, final Writer output) throws IOException {
    if (data != null) {
      write(data.toString(), output);
    }
  }

  /**
   * Writes chars from a <code>CharSequence</code> to bytes on an <code>OutputStream</code> using
   * the default character encoding of the platform.
   * <p>
   * This method uses {@link String#getBytes()}.
   *
   * @param data the <code>CharSequence</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final CharSequence data, final OutputStream output) throws IOException {
    write(data, output, Charset.defaultCharset());
  }

  /**
   * Writes chars from a <code>CharSequence</code> to bytes on an <code>OutputStream</code> using
   * the specified character encoding.
   * <p>
   * This method uses {@link String#getBytes(String)}.
   *
   * @param data the <code>CharSequence</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final CharSequence data, final OutputStream output,
      final Charset encoding) throws IOException {
    if (data != null) {
      write(data.toString(), output, encoding);
    }
  }

  /**
   * Writes chars from a <code>CharSequence</code> to bytes on an <code>OutputStream</code> using
   * the specified character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#getBytes(String)}.
   *
   * @param data the <code>CharSequence</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static void write(final CharSequence data, final OutputStream output,
      final String encoding) throws IOException {
    write(data, output, toCharset(encoding));
  }

  // write String
  // -----------------------------------------------------------------------
  /**
   * Writes chars from a <code>String</code> to a <code>Writer</code>.
   *
   * @param data the <code>String</code> to write, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final String data, final Writer output) throws IOException {
    if (data != null) {
      output.write(data);
    }
  }

  /**
   * Writes chars from a <code>String</code> to bytes on an <code>OutputStream</code> using the
   * default character encoding of the platform.
   * <p>
   * This method uses {@link String#getBytes()}.
   *
   * @param data the <code>String</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final String data, final OutputStream output) throws IOException {
    write(data, output, Charset.defaultCharset());
  }

  /**
   * Writes chars from a <code>String</code> to bytes on an <code>OutputStream</code> using the
   * specified character encoding.
   * <p>
   * This method uses {@link String#getBytes(String)}.
   *
   * @param data the <code>String</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   */
  public static void write(final String data, final OutputStream output, final Charset encoding)
      throws IOException {
    if (data != null) {
      output.write(data.getBytes(encoding));
    }
  }

  /**
   * Writes chars from a <code>String</code> to bytes on an <code>OutputStream</code> using the
   * specified character encoding.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#getBytes(String)}.
   *
   * @param data the <code>String</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static void write(final String data, final OutputStream output, final String encoding)
      throws IOException {
    write(data, output, toCharset(encoding));
  }

  // writeLines
  // -----------------------------------------------------------------------
  /**
   * Writes the <code>toString()</code> value of each item in a collection to an
   * <code>OutputStream</code> line by line, using the default character encoding of the platform
   * and the specified line ending.
   *
   * @param lines the lines to write, null entries produce blank lines
   * @param lineEnding the line separator to use, null is system default
   * @param output the <code>OutputStream</code> to write to, not null, not closed
   * @throws NullPointerException if the output is null
   * @throws IOException if an I/O error occurs
   */
  public static void writeLines(final Collection<?> lines, final String lineEnding,
      final OutputStream output) throws IOException {
    writeLines(lines, lineEnding, output, Charset.defaultCharset());
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to an
   * <code>OutputStream</code> line by line, using the specified character encoding and the
   * specified line ending.
   *
   * @param lines the lines to write, null entries produce blank lines
   * @param lineEnding the line separator to use, null is system default
   * @param output the <code>OutputStream</code> to write to, not null, not closed
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the output is null
   * @throws IOException if an I/O error occurs
   */
  public static void writeLines(final Collection<?> lines, String lineEnding,
      final OutputStream output, final Charset encoding) throws IOException {
    if (lines == null) {
      return;
    }
    if (lineEnding == null) {
      lineEnding = LINE_SEPARATOR;
    }
    for (Object line : lines) {
      if (line != null) {
        output.write(line.toString().getBytes(encoding));
      }
      output.write(lineEnding.getBytes(encoding));
    }
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to an
   * <code>OutputStream</code> line by line, using the specified character encoding and the
   * specified line ending.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   *
   * @param lines the lines to write, null entries produce blank lines
   * @param lineEnding the line separator to use, null is system default
   * @param output the <code>OutputStream</code> to write to, not null, not closed
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the output is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static void writeLines(final Collection<?> lines, final String lineEnding,
      final OutputStream output, final String encoding) throws IOException {
    writeLines(lines, lineEnding, output, toCharset(encoding));
  }

  /**
   * Writes the <code>toString()</code> value of each item in a collection to a <code>Writer</code>
   * line by line, using the specified line ending.
   *
   * @param lines the lines to write, null entries produce blank lines
   * @param lineEnding the line separator to use, null is system default
   * @param writer the <code>Writer</code> to write to, not null, not closed
   * @throws NullPointerException if the input is null
   * @throws IOException if an I/O error occurs
   */
  public static void writeLines(final Collection<?> lines, String lineEnding, final Writer writer)
      throws IOException {
    if (lines == null) {
      return;
    }
    if (lineEnding == null) {
      lineEnding = LINE_SEPARATOR;
    }
    for (Object line : lines) {
      if (line != null) {
        writer.write(line.toString());
      }
      writer.write(lineEnding);
    }
  }

  // copy from InputStream
  // -----------------------------------------------------------------------
  /**
   * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * Large streams (over 2GB) will return a bytes copied value of <code>-1</code> after the copy has
   * completed since the correct number of bytes cannot be returned as an int. For large streams use
   * the <code>copyLarge(InputStream, OutputStream)</code> method.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static int copy(final InputStream input, final OutputStream output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final InputStream input, final OutputStream output)
      throws IOException {
    return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method uses the provided buffer, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @param buffer the buffer to use for the copy
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final InputStream input, final OutputStream output,
      final byte[] buffer) throws IOException {
    long count = 0;
    int n = 0;
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy some or all bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>, optionally skipping input bytes.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @param inputOffset : number of bytes to skip from input before copying -ve values are ignored
   * @param length : number of bytes to copy. -ve means all
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final InputStream input, final OutputStream output,
      final long inputOffset, final long length) throws IOException {
    return copyLarge(input, output, inputOffset, length, new byte[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Copy some or all bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>, optionally skipping input bytes.
   * <p>
   * This method uses the provided buffer, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @param inputOffset : number of bytes to skip from input before copying -ve values are ignored
   * @param length : number of bytes to copy. -ve means all
   * @param buffer the buffer to use for the copy
   *
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final InputStream input, final OutputStream output,
      final long inputOffset, final long length, final byte[] buffer) throws IOException {
    if (inputOffset > 0) {
      skipFully(input, inputOffset);
    }
    if (length == 0) {
      return 0;
    }
    final int bufferLength = buffer.length;
    int bytesToRead = bufferLength;
    if (length > 0 && length < bufferLength) {
      bytesToRead = (int) length;
    }
    int read;
    long totalRead = 0;
    while (bytesToRead > 0 && EOF != (read = input.read(buffer, 0, bytesToRead))) {
      output.write(buffer, 0, read);
      totalRead += read;
      if (length > 0) { // only adjust length if not reading to the end
        // Note the cast must work because buffer.length is an integer
        bytesToRead = (int) Math.min(length - totalRead, bufferLength);
      }
    }
    return totalRead;
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a <code>Writer</code> using the default
   * character encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static void copy(final InputStream input, final Writer output) throws IOException {
    copy(input, output, Charset.defaultCharset());
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a <code>Writer</code> using the
   * specified character encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static void copy(final InputStream input, final Writer output, final Charset encoding)
      throws IOException {
    InputStreamReader in = new InputStreamReader(input, encoding);
    copy(in, output);
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a <code>Writer</code> using the
   * specified character encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static void copy(final InputStream input, final Writer output, final String encoding)
      throws IOException {
    copy(input, output, toCharset(encoding));
  }

  // copy from Reader
  // -----------------------------------------------------------------------
  /**
   * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   * Large streams (over 2GB) will return a chars copied value of <code>-1</code> after the copy has
   * completed since the correct number of chars cannot be returned as an int. For large streams use
   * the <code>copyLarge(Reader, Writer)</code> method.
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied, or -1 if &gt; Integer.MAX_VALUE
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static int copy(final Reader input, final Writer output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final Reader input, final Writer output) throws IOException {
    return copyLarge(input, output, new char[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Copy chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method uses the provided buffer, so there is no need to use a <code>BufferedReader</code>.
   * <p>
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @param buffer the buffer to be used for the copy
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final Reader input, final Writer output, final char[] buffer)
      throws IOException {
    long count = 0;
    int n = 0;
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy some or all chars from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>, optionally skipping input chars.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   * The buffer size is given by {@link #DEFAULT_BUFFER_SIZE}.
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @param inputOffset : number of chars to skip from input before copying -ve values are ignored
   * @param length : number of chars to copy. -ve means all
   * @return the number of chars copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final Reader input, final Writer output, final long inputOffset,
      final long length) throws IOException {
    return copyLarge(input, output, inputOffset, length, new char[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Copy some or all chars from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>, optionally skipping input chars.
   * <p>
   * This method uses the provided buffer, so there is no need to use a <code>BufferedReader</code>.
   * <p>
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @param inputOffset : number of chars to skip from input before copying -ve values are ignored
   * @param length : number of chars to copy. -ve means all
   * @param buffer the buffer to be used for the copy
   * @return the number of chars copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static long copyLarge(final Reader input, final Writer output, final long inputOffset,
      final long length, final char[] buffer) throws IOException {
    if (inputOffset > 0) {
      skipFully(input, inputOffset);
    }
    if (length == 0) {
      return 0;
    }
    int bytesToRead = buffer.length;
    if (length > 0 && length < buffer.length) {
      bytesToRead = (int) length;
    }
    int read;
    long totalRead = 0;
    while (bytesToRead > 0 && EOF != (read = input.read(buffer, 0, bytesToRead))) {
      output.write(buffer, 0, read);
      totalRead += read;
      if (length > 0) { // only adjust length if not reading to the end
        // Note the cast must work because buffer.length is an integer
        bytesToRead = (int) Math.min(length - totalRead, buffer.length);
      }
    }
    return totalRead;
  }

  /**
   * Copy chars from a <code>Reader</code> to bytes on an <code>OutputStream</code> using the
   * default character encoding of the platform, and calling flush.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   * Due to the implementation of OutputStreamWriter, this method performs a flush.
   * <p>
   * This method uses {@link OutputStreamWriter}.
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static void copy(final Reader input, final OutputStream output) throws IOException {
    copy(input, output, Charset.defaultCharset());
  }

  /**
   * Copy chars from a <code>Reader</code> to bytes on an <code>OutputStream</code> using the
   * specified character encoding, and calling flush.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * </p>
   * <p>
   * Due to the implementation of OutputStreamWriter, this method performs a flush.
   * </p>
   * <p>
   * This method uses {@link OutputStreamWriter}.
   * </p>
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  public static void copy(final Reader input, final OutputStream output, final Charset encoding)
      throws IOException {
    OutputStreamWriter out = new OutputStreamWriter(output, encoding);
    copy(input, out);
    // XXX Unless anyone is planning on rewriting OutputStreamWriter,
    // we have to flush here.
    out.flush();
  }

  /**
   * Copy chars from a <code>Reader</code> to bytes on an <code>OutputStream</code> using the
   * specified character encoding, and calling flush.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedReader</code>.
   * <p>
   * Character encoding names can be found at
   * <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * Due to the implementation of OutputStreamWriter, this method performs a flush.
   * <p>
   * This method uses {@link OutputStreamWriter}.
   *
   * @param input the <code>Reader</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @throws UnsupportedCharsetException thrown instead of {@link UnsupportedEncodingException} in
   *         version 2.2 if the encoding is not supported.
   */
  public static void copy(final Reader input, final OutputStream output, final String encoding)
      throws IOException {
    copy(input, output, toCharset(encoding));
  }

  // content equals
  // -----------------------------------------------------------------------
  /**
   * Compare the contents of two Streams to determine if they are equal or not.
   * <p>
   * This method buffers the input internally using <code>BufferedInputStream</code> if they are not
   * already buffered.
   *
   * @param input1 the first stream
   * @param input2 the second stream
   * @return true if the content of the streams are equal or they both don't exist, false otherwise
   * @throws NullPointerException if either input is null
   * @throws IOException if an I/O error occurs
   */
  public static boolean contentEquals(InputStream input1, InputStream input2) throws IOException {
    if (!(input1 instanceof BufferedInputStream)) {
      input1 = new BufferedInputStream(input1);
    }
    if (!(input2 instanceof BufferedInputStream)) {
      input2 = new BufferedInputStream(input2);
    }

    int ch = input1.read();
    while (EOF != ch) {
      int ch2 = input2.read();
      if (ch != ch2) {
        return false;
      }
      ch = input1.read();
    }

    int ch2 = input2.read();
    return ch2 == EOF;
  }

  /**
   * Compare the contents of two Readers to determine if they are equal or not.
   * <p>
   * This method buffers the input internally using <code>BufferedReader</code> if they are not
   * already buffered.
   *
   * @param input1 the first reader
   * @param input2 the second reader
   * @return true if the content of the readers are equal or they both don't exist, false otherwise
   * @throws NullPointerException if either input is null
   * @throws IOException if an I/O error occurs
   */
  public static boolean contentEquals(Reader input1, Reader input2) throws IOException {

    input1 = toBufferedReader(input1);
    input2 = toBufferedReader(input2);

    int ch = input1.read();
    while (EOF != ch) {
      int ch2 = input2.read();
      if (ch != ch2) {
        return false;
      }
      ch = input1.read();
    }

    int ch2 = input2.read();
    return ch2 == EOF;
  }

  /**
   * Compare the contents of two Readers to determine if they are equal or not, ignoring EOL
   * characters.
   * <p>
   * This method buffers the input internally using <code>BufferedReader</code> if they are not
   * already buffered.
   *
   * @param input1 the first reader
   * @param input2 the second reader
   * @return true if the content of the readers are equal (ignoring EOL differences), false
   *         otherwise
   * @throws NullPointerException if either input is null
   * @throws IOException if an I/O error occurs
   */
  public static boolean contentEqualsIgnoreEOL(final Reader input1, final Reader input2)
      throws IOException {
    BufferedReader br1 = toBufferedReader(input1);
    BufferedReader br2 = toBufferedReader(input2);

    String line1 = br1.readLine();
    String line2 = br2.readLine();
    while (line1 != null && line2 != null && line1.equals(line2)) {
      line1 = br1.readLine();
      line2 = br2.readLine();
    }
    return line1 == null ? line2 == null ? true : false : line1.equals(line2);
  }

  /**
   * Skip bytes from an input byte stream. This implementation guarantees that it will read as many
   * bytes as possible before giving up; this may not always be the case for subclasses of
   * {@link Reader}.
   *
   * @param input byte stream to skip
   * @param toSkip number of bytes to skip.
   * @return number of bytes actually skipped.
   *
   * @see InputStream#skip(long)
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if toSkip is negative
   */
  public static long skip(final InputStream input, final long toSkip) throws IOException {
    if (toSkip < 0) {
      throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
    }
    /*
     * N.B. no need to synchronize this because: - we don't care if the buffer is created multiple
     * times (the data is ignored) - we always use the same size buffer, so if it it is recreated it
     * will still be OK (if the buffer size were variable, we would need to synch. to ensure some
     * other thread did not create a smaller one)
     */
    if (SKIP_BYTE_BUFFER == null) {
      SKIP_BYTE_BUFFER = new byte[SKIP_BUFFER_SIZE];
    }
    long remain = toSkip;
    while (remain > 0) {
      long n = input.read(SKIP_BYTE_BUFFER, 0, (int) Math.min(remain, SKIP_BUFFER_SIZE));
      if (n < 0) { // EOF
        break;
      }
      remain -= n;
    }
    return toSkip - remain;
  }

  /**
   * Skip characters from an input character stream. This implementation guarantees that it will
   * read as many characters as possible before giving up; this may not always be the case for
   * subclasses of {@link Reader}.
   *
   * @param input character stream to skip
   * @param toSkip number of characters to skip.
   * @return number of characters actually skipped.
   *
   * @see Reader#skip(long)
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if toSkip is negative
   */
  public static long skip(final Reader input, final long toSkip) throws IOException {
    if (toSkip < 0) {
      throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
    }
    /*
     * N.B. no need to synchronize this because: - we don't care if the buffer is created multiple
     * times (the data is ignored) - we always use the same size buffer, so if it it is recreated it
     * will still be OK (if the buffer size were variable, we would need to synch. to ensure some
     * other thread did not create a smaller one)
     */
    if (SKIP_CHAR_BUFFER == null) {
      SKIP_CHAR_BUFFER = new char[SKIP_BUFFER_SIZE];
    }
    long remain = toSkip;
    while (remain > 0) {
      long n = input.read(SKIP_CHAR_BUFFER, 0, (int) Math.min(remain, SKIP_BUFFER_SIZE));
      if (n < 0) { // EOF
        break;
      }
      remain -= n;
    }
    return toSkip - remain;
  }

  /**
   * Skip the requested number of bytes or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link InputStream#skip(long)} may not skip as many bytes
   * as requested (most likely because of reaching EOF).
   *
   * @param input stream to skip
   * @param toSkip the number of bytes to skip
   * @see InputStream#skip(long)
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if toSkip is negative
   * @throws EOFException if the number of bytes skipped was incorrect
   */
  public static void skipFully(final InputStream input, final long toSkip) throws IOException {
    if (toSkip < 0) {
      throw new IllegalArgumentException("Bytes to skip must not be negative: " + toSkip);
    }
    long skipped = skip(input, toSkip);
    if (skipped != toSkip) {
      throw new EOFException("Bytes to skip: " + toSkip + " actual: " + skipped);
    }
  }

  /**
   * Skip the requested number of characters or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link Reader#skip(long)} may not skip as many characters
   * as requested (most likely because of reaching EOF).
   *
   * @param input stream to skip
   * @param toSkip the number of characters to skip
   * @see Reader#skip(long)
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if toSkip is negative
   * @throws EOFException if the number of characters skipped was incorrect
   */
  public static void skipFully(final Reader input, final long toSkip) throws IOException {
    long skipped = skip(input, toSkip);
    if (skipped != toSkip) {
      throw new EOFException("Chars to skip: " + toSkip + " actual: " + skipped);
    }
  }

  /**
   * Read characters from an input character stream. This implementation guarantees that it will
   * read as many characters as possible before giving up; this may not always be the case for
   * subclasses of {@link Reader}.
   *
   * @param input where to read input from
   * @param buffer destination
   * @param offset inital offset into buffer
   * @param length length to read, must be >= 0
   * @return actual length read; may be less than requested if EOF was reached
   * @throws IOException if a read error occurs
   */
  public static int read(final Reader input, final char[] buffer, final int offset,
      final int length) throws IOException {
    if (length < 0) {
      throw new IllegalArgumentException("Length must not be negative: " + length);
    }
    int remaining = length;
    while (remaining > 0) {
      int location = length - remaining;
      int count = input.read(buffer, offset + location, remaining);
      if (EOF == count) { // EOF
        break;
      }
      remaining -= count;
    }
    return length - remaining;
  }

  /**
   * Read characters from an input character stream. This implementation guarantees that it will
   * read as many characters as possible before giving up; this may not always be the case for
   * subclasses of {@link Reader}.
   *
   * @param input where to read input from
   * @param buffer destination
   * @return actual length read; may be less than requested if EOF was reached
   * @throws IOException if a read error occurs
   */
  public static int read(final Reader input, final char[] buffer) throws IOException {
    return read(input, buffer, 0, buffer.length);
  }

  /**
   * Read bytes from an input stream. This implementation guarantees that it will read as many bytes
   * as possible before giving up; this may not always be the case for subclasses of
   * {@link InputStream}.
   *
   * @param input where to read input from
   * @param buffer destination
   * @param offset inital offset into buffer
   * @param length length to read, must be >= 0
   * @return actual length read; may be less than requested if EOF was reached
   * @throws IOException if a read error occurs
   */
  public static int read(final InputStream input, final byte[] buffer, final int offset,
      final int length) throws IOException {
    if (length < 0) {
      throw new IllegalArgumentException("Length must not be negative: " + length);
    }
    int remaining = length;
    while (remaining > 0) {
      int location = length - remaining;
      int count = input.read(buffer, offset + location, remaining);
      if (EOF == count) { // EOF
        break;
      }
      remaining -= count;
    }
    return length - remaining;
  }

  /**
   * Read bytes from an input stream. This implementation guarantees that it will read as many bytes
   * as possible before giving up; this may not always be the case for subclasses of
   * {@link InputStream}.
   *
   * @param input where to read input from
   * @param buffer destination
   * @return actual length read; may be less than requested if EOF was reached
   * @throws IOException if a read error occurs
   */
  public static int read(final InputStream input, final byte[] buffer) throws IOException {
    return read(input, buffer, 0, buffer.length);
  }

  /**
   * Read the requested number of characters or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link Reader#read(char[], int, int)} may not read as many
   * characters as requested (most likely because of reaching EOF).
   *
   * @param input where to read input from
   * @param buffer destination
   * @param offset inital offset into buffer
   * @param length length to read, must be >= 0
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if length is negative
   * @throws EOFException if the number of characters read was incorrect
   */
  public static void readFully(final Reader input, final char[] buffer, final int offset,
      final int length) throws IOException {
    int actual = read(input, buffer, offset, length);
    if (actual != length) {
      throw new EOFException("Length to read: " + length + " actual: " + actual);
    }
  }

  /**
   * Read the requested number of characters or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link Reader#read(char[], int, int)} may not read as many
   * characters as requested (most likely because of reaching EOF).
   *
   * @param input where to read input from
   * @param buffer destination
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if length is negative
   * @throws EOFException if the number of characters read was incorrect
   */
  public static void readFully(final Reader input, final char[] buffer) throws IOException {
    readFully(input, buffer, 0, buffer.length);
  }

  /**
   * Read the requested number of bytes or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link InputStream#read(byte[], int, int)} may not read as
   * many bytes as requested (most likely because of reaching EOF).
   *
   * @param input where to read input from
   * @param buffer destination
   * @param offset inital offset into buffer
   * @param length length to read, must be >= 0
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if length is negative
   * @throws EOFException if the number of bytes read was incorrect
   */
  public static void readFully(final InputStream input, final byte[] buffer, final int offset,
      final int length) throws IOException {
    int actual = read(input, buffer, offset, length);
    if (actual != length) {
      throw new EOFException("Length to read: " + length + " actual: " + actual);
    }
  }

  /**
   * Read the requested number of bytes or fail if there are not enough left.
   * <p>
   * This allows for the possibility that {@link InputStream#read(byte[], int, int)} may not read as
   * many bytes as requested (most likely because of reaching EOF).
   *
   * @param input where to read input from
   * @param buffer destination
   *
   * @throws IOException if there is a problem reading the file
   * @throws IllegalArgumentException if length is negative
   * @throws EOFException if the number of bytes read was incorrect
   */
  public static void readFully(final InputStream input, final byte[] buffer) throws IOException {
    readFully(input, buffer, 0, buffer.length);
  }

  /**
   * Returns a Charset for the named charset. If the name is null, return the default Charset.
   *
   * @param charset The name of the requested charset, may be null.
   * @return a Charset for the named charset
   * @throws UnsupportedCharsetException If the named charset is unavailable
   */
  public static Charset toCharset(final String charset) {
    return charset == null ? Charset.defaultCharset() : Charset.forName(charset);
  }
}
