package com.jingoal.dfsclient.load;

/**
 * Defines the set of all configuration dependencies required for the DfsNodeLocator algorithm to
 * run.
 */
public interface NodeLocatorCfg<T> {
  /**
   * Returns a uniquely identifying key, suitable for hashing by the NodeLocator algorithm.
   *
   * @param node The DfsServerNode to use to form the unique identifier.
   * @param repetition The repetition number for the particular node in question (0 is the first
   *        repetition).
   * @return The key that represents the specific repetition of the node.
   */
  public String getKeyForNode(T node, int repetition);

  /**
   * Returns the number of discrete hashes that should be defined for each node in the continuum.
   *
   * @return a value greater than 0.
   */
  public int getNodeRepetitions();
}
