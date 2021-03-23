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
  public TextWrap getTextWrap(U16Vec data) {
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
   * @param data the Vec containing the characters as U16 values of the String.
   * @return a String using the characters from {@link #startOffset} to {@link #endOffset}
   *
   * @see #getTextWrap(U16Vec)
   */
  public String getString(U16Vec data) {
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
   * Returns a stream of the bytes between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of the bytes between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#data()
   * @see U8Vec#getByte(long)
   */
  public IntStream bytes(U8Vec data) {
    return offsets().mapToInt(data::getByte);
  }

  /**
   * Returns a stream of the shorts between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of the shorts between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#data()
   * @see U8Vec#getByte(long)
   */
  public IntStream shorts(U16Vec data) {
    return offsets().mapToInt(data::getShort);
  }

  /**
   * Returns a stream of the chars between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of the chars between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#data()
   * @see U8Vec#getByte(long)
   */
  public IntStream chars(U16Vec data) {
    return offsets().mapToInt(data::getChar);
  }

  /**
   * Returns a stream of the ints between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of the ints between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#data()
   * @see U8Vec#getByte(long)
   */
  public IntStream ints(U32Vec data) {
    return offsets().mapToInt(data::getInt);
  }

  /**
   * Returns a stream of the floats between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of the floats between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#data()
   * @see U8Vec#getByte(long)
   */
  public DoubleStream floats(U32Vec data) {
    return offsets().mapToDouble(data::getFloat);
  }

  /**
   * Returns a stream of the longs between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of the longs between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#data()
   * @see U8Vec#getByte(long)
   */
  public LongStream longs(U64Vec data) {
    return offsets().map(data::getLong);
  }

  /**
   * Returns a stream of the doubles between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of the doubles between [{@code startOffset}, {@code endOffset}[
   * @throws NullPointerException if one of the value is {@code null}
   *
   * @see ListVec#data()
   * @see U8Vec#getByte(long)
   */
  public DoubleStream floats(U64Vec data) {
    return offsets().mapToDouble(data::getDouble);
  }

  /**
   * Returns a stream of {@link TextWrap}s between [{@code startOffset}, {@code endOffset}[
   * @param data the Vec containing the data of the ListVec
   * @return a stream of {@link TextWrap}s between [{@code startOffset}, {@code endOffset}[
   *
   * @see ListVec#data()
   * @see ListVec#getString(long)
   */
  public Stream<TextWrap> textWraps(ListVec<U16Vec> data) {
    return offsets().mapToObj(index -> data.getTextWrap(index));
  }
}