package com.jingoal.dfsclient.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author yhhan
 *
 */
public final class FileUtils {

  private FileUtils() {}

  public static final Integer PATH_LEVEL3 = 2; // 3级目录
  public static final Integer PATH_LEVEL4 = 3; // 4级目录
  public static final Integer PATH_LEVEL5 = 4; // 5级目录
  public static final Integer PATH_LEVEL6 = 5; // 6级目录

  private static StringBuilder genNewDirPath(final String baseDir, final long domain) {
    return new StringBuilder(baseDir).append(File.separator).append(domain);
  }

  private static String getFilePath(final String baseDir, final Integer pathVersion,
      final long domain, final String fn, final Integer dig) {
    int digit = checkDigit(dig);

    String path = null;
    if (pathVersion == null) {
      path = genNewDirPath(baseDir, domain).append(File.separator).append(fn).toString();
    } else if (pathVersion == PATH_LEVEL3) { // 直接使用3级目录格式
      path = getPathLevel3(baseDir, domain, fn, digit);
    } else if (pathVersion == PATH_LEVEL4) { // 直接使用4级目录格式
      path = getPathLevel4(baseDir, domain, fn, digit);
    } else if (pathVersion == PATH_LEVEL5) { // 直接使用5级目录格式
      path = getPathLevel5(baseDir, domain, fn, digit);
    } else if (pathVersion == PATH_LEVEL6) { // 直接使用6级目录格式
      path = getPathLevel6(baseDir, domain, fn, digit);
    } else {
      path = genNewDirPath(baseDir, domain).append(File.separator).append(fn).toString();
    }
    return path;
  }

  private static int checkDigit(Integer digit) {
    if (digit == null) {
      digit = 2;
    }
    if (digit < 2) {
      digit = 2;
    }
    if (digit > 4) {
      digit = 4;
    }
    return digit;
  }

  private static String getDummy(final int digit) {
    if (digit == 3) {
      return "000";
    }
    if (digit == 4) {
      return "0000";
    }
    return "00";
  }

  private static String getPathLevel3(final String baseDir, final long domain, final String fn,
      final int digit) {
    StringBuilder sb = new StringBuilder(baseDir).append(File.separator).append("g")
        .append(domain % 10000).append(File.separator).append(domain);
    if (fn != null && !"".equals(fn.trim())) {
      sb.append(File.separator)
          .append((fn.length() >= digit ? fn.substring(0, digit) : getDummy(digit)))
          .append(File.separator).append(fn);
    }
    return sb.toString();
  }

  private static String getPathLevel4(final String baseDir, final long domain, final String fn,
      final int digit) {
    StringBuilder sb = new StringBuilder(baseDir).append(File.separator).append("g")
        .append(domain % 10000).append(File.separator).append(domain);
    if (fn != null && !"".equals(fn.trim())) {
      sb.append(File.separator)
          .append((fn.length() >= digit ? fn.substring(0, digit) : getDummy(digit)))
          .append(File.separator)
          .append(
              (fn.length() >= (2 * digit) ? fn.substring((digit), (2 * digit)) : getDummy(digit)))
          .append(File.separator).append(fn);
    }
    return sb.toString();
  }

  private static String getPathLevel5(final String baseDir, final long domain, final String fn,
      final int digit) {
    StringBuilder sb = new StringBuilder(baseDir).append(File.separator).append("g")
        .append(domain % 10000).append(File.separator).append(domain);
    if (fn != null && !"".equals(fn.trim())) {
      sb.append(File.separator)
          .append((fn.length() >= digit ? fn.substring(0, digit) : getDummy(digit)))
          .append(File.separator)
          .append((fn.length() >= (2 * digit) ? fn.substring(digit, (2 * digit)) : getDummy(digit)))
          .append(File.separator).append((fn.length() >= (3 * digit)
              ? fn.substring((2 * digit), (3 * digit)) : getDummy(digit)))
          .append(File.separator).append(fn);
    }
    return sb.toString();
  }

  private static String getPathLevel6(final String baseDir, final long domain, final String fn,
      final int digit) {
    StringBuilder sb = new StringBuilder(baseDir).append(File.separator).append("g")
        .append(domain % 10000).append(File.separator).append(domain);
    if (fn != null && !"".equals(fn.trim())) {
      sb.append(File.separator)
          .append((fn.length() >= digit ? fn.substring(0, digit) : getDummy(digit)))
          .append(File.separator)
          .append((fn.length() >= (2 * digit) ? fn.substring(digit, (2 * digit)) : getDummy(digit)))
          .append(File.separator)
          .append((fn.length() >= (3 * digit) ? fn.substring((2 * digit), (3 * digit))
              : getDummy(digit)))
          .append(File.separator).append((fn.length() >= (4 * digit)
              ? fn.substring((3 * digit), (4 * digit)) : getDummy(digit)))
          .append(File.separator).append(fn);
    }
    return sb.toString();
  }

  /**
   * 取得用来写入的File。
   *
   * @param baseDir
   * @param domain
   * @param fn
   * @return
   */
  public static File getAndEnsureFile4Write(final String baseDir, final Integer pathVersion,
      final long domain, final String fn, final Integer dig) {
    if (fn == null || "".equals(fn.trim())) {
      return null;
    }
    String path = getFilePath(baseDir, pathVersion, domain, fn, dig);

    String dir = path.substring(0, path.length() - fn.length() - 1);
    File f = new File(dir);
    if (!f.exists()) {
      f.mkdirs();
    }
    return new File(path);
  }

  /**
   * 取得用来读取或者删除的File，先在refined的路径中寻找，如果没有再在旧路径中寻找。 找到文件且存在，返回File对象，不存在返回null。
   *
   * @param baseDir
   * @param domain
   * @param fn
   * @return
   * @throws FileNotFoundException
   */
  public static File getFile4ReadAndDelete(final String baseDir, final Integer pathVersion,
      final long domain, final String fn, final Integer dig) throws FileNotFoundException {
    if (fn == null || "".equals(fn.trim())) {
      return null;
    }
    String path = getFilePath(baseDir, pathVersion, domain, fn, dig);

    File f = new File(path);
    if (f.exists()) {
      return f;
    } else {
      throw new FileNotFoundException("file " + f.getAbsolutePath() + " not exist.");
    }
  }

  /**
   * Copy bytes from a <code>InputStream</code> to an <code>OutputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @throws IOException if an I/O error occurs
   */
  public static void copy(final InputStream input, final OutputStream output) throws IOException {
    try {
      IOUtils.copy(input, output);
    } finally {
      try {
        input.close();
      } finally {
        output.close();
      }
    }
  }

  /**
   * Copy bytes from a <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method uses the provided buffer, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   *
   * @param input the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @param bufferSize the buffer size to use for the copy
   * @throws IOException if an I/O error occurs
   */
  public static void copyLarge(final InputStream input, final OutputStream output, int bufferSize)
      throws IOException {
    try {
      IOUtils.copyLarge(input, output, new byte[bufferSize]);
    } finally {
      try {
        input.close();
      } finally {
        output.close();
      }
    }
  }
}
