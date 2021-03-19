package com.github.forax.tomahawk.vec;

import static java.util.Objects.requireNonNull;

import jdk.incubator.foreign.MemorySegment;

public class ValuesBox implements ValuesExtractor {
  public boolean validity;
  public long startOffset;
  public long endOffset;

  @Override
  public void consume(boolean validity, long startOffset, long endOffset) {
    this.validity = validity;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  public String getString(U16Vec dataset) {
    requireNonNull(dataset);
    if (!validity) {
      return null;
    }
    var dataSegment = VecImpl.impl(dataset).dataSegment();
    var start = startOffset;
    var end = endOffset;
    var length = end - start;
    var charArray = new char[(int)length];
    MemorySegment.ofArray(charArray).copyFrom(dataSegment.asSlice(start << 1L, length << 1L));
    return new String(charArray);
  }

  public int[] getIntArray(U32Vec dataset) {
    requireNonNull(dataset);
    if (!validity) {
      return null;
    }
    var dataSegment = VecImpl.impl(dataset).dataSegment();
    var start = startOffset;
    var end = endOffset;
    var length = end - start;
    var intArray = new int[(int)length];
    MemorySegment.ofArray(intArray).copyFrom(dataSegment.asSlice(start << 2L, length << 2L));
    return intArray;
  }
}