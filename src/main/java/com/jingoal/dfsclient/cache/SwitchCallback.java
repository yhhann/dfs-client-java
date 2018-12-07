package com.jingoal.dfsclient.cache;

/**
 * To carry out the catch switch operation.
 *
 */
public interface SwitchCallback {
  /**
   * Cache switch according to the listener state.
   *
   * @param pingpang
   */
  public void onSwitch(long pingpang);
}
