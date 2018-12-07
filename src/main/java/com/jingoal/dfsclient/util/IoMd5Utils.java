package com.jingoal.dfsclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * InputStream md5 utils.
 */
public final class IoMd5Utils {

  private static final int DEFAULT_BUFFER_SIZE = 255 * 1024;
  private static final int EOF = -1;

  private IoMd5Utils() {}

  /**
   * Get the instance of MessageDigest.
   *
   * @return
   */
  public static MessageDigest getDigest() {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.reset();
      return md5;
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException("No MD5!");
    }
  }

  /**
   * Write the InputStream to OutputStream and md5 to a hexadecimal string.
   *
   * @param input the InputStream to md5
   * @param output the OutputStream
   * @param messageDigester the instance of MessageDigest
   * @return
   * @throws IOException
   */
  public static long copy(final InputStream input, final OutputStream output,
      final MessageDigest messageDigester) throws IOException {
    long count = 0;
    int n = 0;
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
      messageDigester.update(buffer, 0, n);
    }
    return count;
  }

  /**
   * Md5 the given InputStream to a hexadecimal string.
   *
   * @param input the given InputStream to md5
   * @param messageDigester the instance of MessageDigest
   * @return
   * @throws IOException
   */
  private static long read(final InputStream input, final MessageDigest messageDigester)
      throws IOException {
    long count = 0;
    int n = 0;
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    while (EOF != (n = input.read(buffer))) {
      count += n;
      messageDigester.update(buffer, 0, n);
    }
    return count;
  }

  /**
   * Md5 the given byte array to a hexadecimal string.
   *
   * @param byteArray the byte array to md5
   * @return
   */
  public static String md5(final byte[] byteArray) {
    MessageDigest _messageDigester = getDigest();
    _messageDigester.update(byteArray);
    String md5 = toHex(_messageDigester.digest());
    _messageDigester = null;
    return md5;
  }

  /**
   * Md5 the given file to a hexadecimal string.
   *
   * @param file the file to md5
   * @return
   */
  public static String md5(final File file) {
    InputStream fin = null;
    try {
      fin = new FileInputStream(file);
      MessageDigest _messageDigester = getDigest();
      read(fin, _messageDigester);
      String md5 = toHex(_messageDigester.digest());
      _messageDigester = null;
      return md5;
    } catch (FileNotFoundException e) {
      return "";
    } catch (IOException e) {
      return "";
    } finally {
      IOUtils.closeQuietly(fin);
    }
  }

  /**
   * Md5 the given InputStream.
   *
   * @param input the given InputStream to md5.
   * @return StreamData Record the md5 value and inputstream byte size.
   * @throws IOException
   */
  public static StreamData md5(final InputStream input) {
    StreamData obj = new StreamData();
    try {
      MessageDigest _messageDigester = getDigest();
      long size = read(input, _messageDigester);
      String md5 = toHex(_messageDigester.digest());
      _messageDigester = null;
      obj.setMd5(md5);
      obj.setSize(size);
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    } finally {
      IOUtils.closeQuietly(input);
    }
    return obj;
  }

  /**
   * Record the md5 value and inputstream byte size.
   *
   *
   */
  public static class StreamData {
    private long size;
    private String md5;

    public long getSize() {
      return size;
    }

    public void setSize(final long size) {
      this.size = size;
    }

    public String getMd5() {
      return md5;
    }

    public void setMd5(final String md5) {
      this.md5 = md5;
    }
  }

  /**
   * Converts the given byte buffer to a hexadecimal string using
   * {@link java.lang.Integer#toHexString(int)}.
   *
   * @param b the bytes to convert to hex
   * @return a String containing the hex representation of the given bytes.
   */
  public static String toHex(final byte b[]) {
    StringBuilder buf = new StringBuilder();

    for (byte element : b) {
      String s = Integer.toHexString(0xff & element);

      if (s.length() < 2) {
        buf.append("0");
      }
      buf.append(s);
    }

    return buf.toString();
  }

  /**
   * Produce hex representation of the MD5 digest of a byte array
   *
   * @param data bytes to digest
   * @return hex string of the MD5 digest
   */
  public static String hexMD5(final byte[] data) {
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");

      md5.reset();
      md5.update(data);
      byte digest[] = md5.digest();

      return toHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error - this implementation of Java doesn't support MD5.");
    }
  }

  /**
   * Produce hex representation of the MD5 digest of a byte array.
   *
   * @param buf byte buffer containing the bytes to digest
   * @param offset the position to start reading bytes from
   * @param len the number of bytes to read from the buffer
   * @return hex string of the MD5 digest
   */
  public static String hexMD5(final ByteBuffer buf, final int offset, final int len) {
    byte b[] = new byte[len];
    for (int i = 0; i < len; i++) {
      b[i] = buf.get(offset + i);
    }

    return hexMD5(b);
  }
}
