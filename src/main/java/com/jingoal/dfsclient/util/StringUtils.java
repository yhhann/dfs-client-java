package com.jingoal.dfsclient.util;

/**
 * String handling utils.
 */
public final class StringUtils {

  private StringUtils() {}

  /**
   * <p>
   * Checks if a CharSequence is empty ("") or null.
   * </p>
   *
   * <pre>
   * StringUtils.isEmpty(null)      = true
   * StringUtils.isEmpty("")        = true
   * StringUtils.isEmpty(" ")       = false
   * StringUtils.isEmpty("bob")     = false
   * StringUtils.isEmpty("  bob  ") = false
   * </pre>
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is empty or null
   */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  /**
   * <p>
   * Checks if a CharSequence is not empty ("") and not null.
   * </p>
   *
   * <pre>
   * StringUtils.isNotEmpty(null)      = false
   * StringUtils.isNotEmpty("")        = false
   * StringUtils.isNotEmpty(" ")       = true
   * StringUtils.isNotEmpty("bob")     = true
   * StringUtils.isNotEmpty("  bob  ") = true
   * </pre>
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is not empty and not null
   */
  public static boolean isNotEmpty(final CharSequence cs) {
    return !StringUtils.isEmpty(cs);
  }

  /**
   * <p>
   * Checks if a CharSequence is whitespace, empty ("") or null.
   * </p>
   *
   * <pre>
   * StringUtils.isBlank(null)      = true
   * StringUtils.isBlank("")        = true
   * StringUtils.isBlank(" ")       = true
   * StringUtils.isBlank("bob")     = false
   * StringUtils.isBlank("  bob  ") = false
   * </pre>
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is null, empty or whitespace
   */
  public static boolean isBlank(final CharSequence cs) {
    int strLen;
    if (cs == null || (strLen = cs.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * <p>
   * Checks if a CharSequence is not empty (""), not null and not whitespace only.
   * </p>
   *
   * <pre>
   * StringUtils.isNotBlank(null)      = false
   * StringUtils.isNotBlank("")        = false
   * StringUtils.isNotBlank(" ")       = false
   * StringUtils.isNotBlank("bob")     = true
   * StringUtils.isNotBlank("  bob  ") = true
   * </pre>
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if the CharSequence is not empty and not null and not whitespace
   */
  public static boolean isNotBlank(final CharSequence cs) {
    return !isBlank(cs);
  }

  /**
   * <p>
   * Checks if the CharSequence contains only Unicode digits. A decimal point is not a Unicode digit
   * and returns false.
   * </p>
   *
   * <p>
   * {@code null} will return {@code false}. An empty CharSequence (length()=0) will return
   * {@code false}.
   * </p>
   *
   * <p>
   * Note that the method does not allow for a leading sign, either positive or negative. Also, if a
   * String passes the numeric test, it may still generate a NumberFormatException when parsed by
   * Integer.parseInt or Long.parseLong, e.g. if the value is outside the range for int or long
   * respectively.
   * </p>
   *
   * <pre>
   * StringUtils.isNumeric(null)   = false
   * StringUtils.isNumeric("")     = false
   * StringUtils.isNumeric("  ")   = false
   * StringUtils.isNumeric("123")  = true
   * StringUtils.isNumeric("12 3") = false
   * StringUtils.isNumeric("ab2c") = false
   * StringUtils.isNumeric("12-3") = false
   * StringUtils.isNumeric("12.3") = false
   * StringUtils.isNumeric("-123") = false
   * StringUtils.isNumeric("+123") = false
   * </pre>
   *
   * @param cs the CharSequence to check, may be null
   * @return {@code true} if only contains digits, and is non-null
   */
  public static boolean isNumeric(final CharSequence cs) {
    if (isEmpty(cs)) {
      return false;
    }
    final int sz = cs.length();
    for (int i = 0; i < sz; i++) {
      if (Character.isDigit(cs.charAt(i)) == false) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if a string could be an org.bson.types.ObjectId.
   *
   * @return whether the string could be an object id
   */
  public static boolean isValidObjectId(final String _id) {
    if (_id == null) {
      return false;
    }
    String fid = DuplicateUtil.getRealId(_id);
    final int len = fid.length();
    if (len != 24) {
      return false;
    }

    for (int i = 0; i < len; i++) {
      char c = fid.charAt(i);
      if (c >= '0' && c <= '9') {
        continue;
      }
      if (c >= 'a' && c <= 'f') {
        continue;
      }
      if (c >= 'A' && c <= 'F') {
        continue;
      }

      return false;
    }

    return true;
  }
}
