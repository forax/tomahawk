package com.github.forax.tomahawk.vec;

import jdk.incubator.foreign.MemorySegment;

import java.util.Objects;

import static com.github.forax.tomahawk.vec.VecImpl.U16Impl.CHAR_HANDLE;

/**
 * Lightweight version of a String that only keep a pointer to the memory storage
 * instead of duplicating all characters like a String does.
 *
 * Example, instead of allocating all the Strings to look for a special value
 * <pre>
 *   ListVec&lt;U16Vec&gt; list = ...
 *   list.textWraps().map(TextWrap::toString).anyMatch("Hello"::equals)
 * </pre>
 *
 * It's better to allocate only one TextWrap that contains that special value
 * <pre>
 *   ListVec&lt;U16Vec&gt; list = ...
 *   list.textWraps().anyMatch(TextWrap.from("Hello")::equals)
 * </pre>
 */
public final class TextWrap implements CharSequence {
  private final MemorySegment segment;
  private final long offset;
  private final int length;

  private int hashCode;

  TextWrap(MemorySegment segment, long offset, int length) {
    this.segment = segment;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TextWrap textWrap)) {
      return false;
    }
    return length == textWrap.length &&
        segment.asSlice(offset << 1L, (long) length << 1L).mismatch(textWrap.segment.asSlice(textWrap.offset << 1L, (long) textWrap.length << 1L)) == -1;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      return hashCode = computeHashCode(segment, offset, length) | 0x8000_0000;
    }
    return hashCode;
  }

  private static int computeHashCode(MemorySegment segment, long offset, int length) {
    // String.hashCode(), maybe use a better hash function
    var hash = 0;
    for(var i = 0; i < length; i ++) {
      hash = 31 * hash + (char) CHAR_HANDLE.get(segment, offset << 1L + i << 1L);
    }
    return hash;
  }

  public int length() {
    return length;
  }

  public char charAt(int index) {
    Objects.checkIndex(index, length);
    return (char) CHAR_HANDLE.get(segment, offset << 1L + index << 1L);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return toString().subSequence(start, end);
  }

  @Override
  public String toString() {
    var charArray = new char[length];
    MemorySegment.ofArray(charArray).copyFrom(segment.asSlice(offset << 1L, (long) length << 1L));
    return new String(charArray);
  }

  /**
   * Creates a TextWrap from a String by duplicating the content.
   * @param text the string
   * @return a new TextWrap that has the same content as the String taken as argument.
   */
  public static TextWrap from(String text) {
    var segment = MemorySegment.ofArray(text.toCharArray());
    return new TextWrap(segment, 0, text.length());
  }
}
