package com.jingoal.dfsclient.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class CycleIterator<E> implements Iterator<E> {
  private ArrayList<E> aList = new ArrayList<E>();
  private AtomicInteger curIndex = new AtomicInteger(0);
  private Random random = new Random(System.currentTimeMillis());

  public CycleIterator(List<E> aList) {
    if (aList.size() == 0) {
      return;
    }

    this.curIndex.set(random.nextInt(aList.size()));
    Collections.shuffle(aList, random);
    this.aList.addAll(aList);
  }

  @Override
  public boolean hasNext() {
    return this.aList.size() > 0;
  }

  @Override
  public E next() {
    if (this.aList.size() == 0) {
      return null;
    }

    return this.aList.get(this.curIndex.getAndIncrement() % this.aList.size());
  }

  public List<E> Underlying() {
    return new ArrayList<E>(this.aList);
  }

}
