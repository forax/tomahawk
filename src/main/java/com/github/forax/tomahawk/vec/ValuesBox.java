package com.github.forax.tomahawk.vec;

import static java.util.Objects.requireNonNull;

import jdk.incubator.foreign.MemorySegment;

/**
 * A mutable class that represents a nullable list of values.
 *
 * If {@link #validity} is true, the values of the box is stored in between
 * [#startOffset, #endOffset[,
 * otherwise if {@link #validity} is false, the value of the box is {@code null}.
 *
 * @see ListVec#getValues(long, ValuesExtractor)
 */
public class ValuesBox implements ValuesExtractor {
  /**
   * The validity of the value, false means that the value doesn't exist (is null)
   */
  public boolean validity;

  /**
   * The offset of the first value
   */
  public long startOffset;

  /**
   * The offset of the last value + 1
   */
  public long endOffset;

  @Override
  public void consume(boolean validity, long startOffset, long endOffset) {
    this.validity = validity;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  /**
   * Extract the values from {@link #startOffset} to {@link #endOffset} into a String.
   * @param vec the Vec containing the characters of the String.
   * @return a String using the characters from {@link #startOffset} to {@link #endOffset}
   * 
   * @see ListVec#getString(long)
   */
  public String getString(U16Vec vec) {
    requireNonNull(vec);
    if (!validity) {
      return null;
    }
    var dataSegment = VecImpl.impl(vec).dataSegment();
    var start = startOffset;
    var end = endOffset;
    var length = end - start;
    var charArray = new char[(int)length];
    MemorySegment.ofArray(charArray).copyFrom(dataSegment.asSlice(start << 1L, length << 1L));
    return new String(charArray);
  }
}