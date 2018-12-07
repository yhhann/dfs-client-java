package com.jingoal.dfsclient.util;

import java.math.BigDecimal;

public class CompareUtils {
  /**
   * Compares {@code val1} with {@code val2}. Two {@code double} objects that are equal in value but
   * have a different scale (like 2.0 and 2.00) are considered equal by this method. This method is
   * provided in preference to individual methods for each of the six boolean comparison operators (
   * {@literal <}, ==, {@literal >}, {@literal >=}, !=, {@literal <=}). The suggested idiom for
   * performing these comparisons is: {@code (compareTo(x,y)} &lt;<i>op</i>&gt; {@code 0)} , where
   * &lt;<i>op</i>&gt; is one of the six comparison operators.
   *
   * @param val1
   * @param val2
   * @return -1, 0, or 1 as {@code val1} is numerically less than, equal to, or greater than
   *         {@code val2}.
   */
  public static int compareTo(final double val1, final double val2) {
    BigDecimal data1 = new BigDecimal(val1);
    BigDecimal data2 = new BigDecimal(val2);
    return data1.compareTo(data2);
  }
}
