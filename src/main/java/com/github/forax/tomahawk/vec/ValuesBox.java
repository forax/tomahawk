package com.github.forax.tomahawk.vec;

import static java.util.Objects.requireNonNull;

import jdk.incubator.foreign.MemorySegment;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A mutable class that represents a nullable list of values.
 *
 * If {@link #validity} is true, the values of the box is stored in between
 * [#startOffset, #endOffset[,
 * otherwise if {@link #validity} is false, the value of the box is {@code null}.
 *
 * @see ListVec#getValues(long, ValuesBox)
 */
public class ValuesBox {
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

  void fill(boolean validity, long startOffset, long endOffset) {
    this.validity = validity;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
  }

  /**
   * Returns a stream of all the offsets between [{@code startOffset}, {@code endOffset}[
   * @return a stream of all the offsets between [{@code startOffset}, {@code endOffset}[
   */
  public LongStream offsets() {
    return LongStream.range(startOffset, endOffset);
  }
  
  /**
   * Extract the values from {@link #startOffset} to {@link #endOffset} into a {@link TextWrap}.
   *
   * @param data the Vec containing the characters as U16 values of the String.
   * @return a TextWrap wrapping the characters from {@link #startOffset} to {@link #endOffset}
   */
  public TextWrap asTextWrap(U16Vec data) {
    requireNonNull(data);
    if (!validity) {
      return null;
    }
    var dataSegment = VecImpl.impl(data).dataSegment();
    var start = startOffset;
    var end = endOffset;
    int length = (int) (end - start);  // FIXME
    return new TextWrap(dataSegment, start, length);
  }

  /**
   * Extract the values from {@link #startOffset} to {@link #endOffset} into a String.
   * In term of performance, it's often better to use a {@link TextWrap} than a String
   * because unlike a String, a TextWrap do not copy the characters
   *
   * This is semantically equivalent to
   * <pre>
   *   U16Vec element = ...
   *   return asTextWrap(element).toString()
   * </pre>
   *
   * @param data the Vec containing the characters as U16 values of the String.
   * @return a String using the characters from {@link #startOffset} to {@link #endOffset}
   *
   * @see #asTextWrap(U16Vec)
   */
  public String asString(U16Vec data) {
    requireNonNull(data);
    if (!validity) {
      return null;
    }
    var dataSegment = VecImpl.impl(data).dataSegment();
    var start = startOffset;
    var end = endOffset;
    var length = end - start;
    var charArray = new char[(int)length];
    MemorySegment.ofArray(charArray).copyFrom(dataSegment.asSlice(start << 1L, length << 1L));
    return new String(charArray);
  }

  /**
   * Returns a stream of the ints between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the element of the ListVec
   * @return a stream of the ints between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#element()
   * @see U8Vec#getByte(long)
   */
  public IntStream ints(U32Vec data) {
    return offsets().mapToInt(data::getInt);
  }

  /**
   * Returns a stream of the longs between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the element of the ListVec
   * @return a stream of the longs between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#element()
   * @see U8Vec#getByte(long)
   */
  public LongStream longs(U64Vec data) {
    return offsets().map(data::getLong);
  }

  /**
   * Returns a stream of the doubles between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the element of the ListVec
   * @return a stream of the doubles between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#element()
   * @see U8Vec#getByte(long)
   */
  public DoubleStream doubles(U64Vec data) {
    return offsets().mapToDouble(data::getDouble);
  }

  /**
   * Returns a stream of {@link TextWrap}s between [{@code startOffset}, {@code endOffset}[
   * @param list the Vec containing list of strings
   * @return a stream of {@link TextWrap}s between [{@code startOffset}, {@code endOffset}[
   * @throws IllegalStateException if the element is not a {@link U16Vec}
   *
   * @see ListVec#element()
   * @see ListVec#getString(long)
   */
  public Stream<TextWrap> textWraps(ListVec<?> list) {
    return offsets().mapToObj(list::getTextWrap);
  }

  private int length() {
    return (int) (endOffset - startOffset); //FIXME
  }

  public boolean[] booleanArray(U1Vec data) {
    var length = length();
    var array = new boolean[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getBoolean(startOffset + i);
    }
    return array;
  }

  public byte[] byteArray(U8Vec data) {
    var length = length();
    var array = new byte[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getByte(startOffset + i);
    }
    return array;
  }

  public short[] shortArray(U16Vec data) {
    var length = length();
    var array = new short[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getShort(startOffset + i);
    }
    return array;
  }

  public char[] charArray(U16Vec data) {
    var length = length();
    var array = new char[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getChar(startOffset + i);
    }
    return array;
  }

  public int[] intArray(U32Vec data) {
    var length = length();
    var array = new int[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getInt(startOffset + i);
    }
    return array;
  }

  public float[] floatArray(U32Vec data) {
    var length = length();
    var array = new float[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getFloat(startOffset + i);
    }
    return array;
  }

  public long[] longArray(U64Vec data) {
    var length = length();
    var array = new long[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getLong(startOffset + i);
    }
    return array;
  }

  public double[] doubleArray(U64Vec data) {
    var length = length();
    var array = new double[length];
    for(var i = 0; i < length; i++) {
      array[i] = data.getDouble(startOffset + i);
    }
    return array;
  }
}