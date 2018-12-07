package com.jingoal.dfsclient.load;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * A Default implementation of the configuration required for the NodeLocator algorithm to run.
 */
public class DfsNodeLocatorCfg implements NodeLocatorCfg<InetSocketAddress> {

  private int numReps = 160;
  private Map<InetSocketAddress, String> nodeKeys = new HashMap<InetSocketAddress, String>();

  public DfsNodeLocatorCfg() {}

  public DfsNodeLocatorCfg(final int numReps) {
    this.numReps = numReps;
  }

  /**
   * Returns the number of discrete hashes that should be defined for each node in the continuum.
   *
   * @return NUM_REPS repetitions.
   */
  @Override
  public int getNodeRepetitions() {
    return numReps;
  }

  /**
   * Returns a uniquely identifying key, suitable for hashing by the NodeLocator algorithm.
   *
   * @param node The InetSocketAddress to use to form the unique identifier.
   * @param repetition The repetition number for the particular node in question (0 is the first
   *        repetition).
   * @return The key that represents the specific repetition of the node.
   */
  @Override
  public String getKeyForNode(final InetSocketAddress node, final int repetition) {
    String nodeKey = nodeKeys.get(node);
    if (nodeKey == null) {
      nodeKey = node.getHostName() + ":" + node.getPort();
      nodeKeys.put(node, nodeKey);
    }
    return nodeKey + "-" + repetition;
  }
}
