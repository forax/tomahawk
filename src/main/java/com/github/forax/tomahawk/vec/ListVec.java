package com.github.forax.tomahawk.vec;

import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.impl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.util.Objects.requireNonNull;

/**
 * A fixed size list of Vec (a list of list of values)
 *
 * It can be created from several ways
 * <ul>
 *   <li>Using a builder {@link #builder(U1Vec.Builder, U32Vec.Builder, BaseBuilder)}
 *       to append values to a new mapped file
 *   <li>From a validity Vec, an offset Vec and a data Vec {@link #from(U1Vec, U32Vec, Vec)}
 * </ul>
 *
 * It can load and store nulls and list of values
 * <ul>
 *   <li>{@link #getValues(long, ValuesExtractor)} loads a nullable list of values
 *   <li>{@link #getString(long)} which is a convenient method in case the values are {@link U32Vec} of char.
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * To store nulls, this Vec must be constructed with a {code validity} {@link U1Vec bit set} either at construction
 * or using {@link #withValidity(U1Vec)}.
 *
 * @param <V> type of the values
 */
public interface ListVec<V extends Vec> extends Vec {
  /**
   * The data Vec
   * @return the Vec containing all the data values
   */
  V data();

  /**
   * Returns a {@link TextWrap} representing a list of U16 values,
   * if the list is not valid, {@code null} is returned.
   *
   * Convenient method equivalent to
   * <pre>
   *   var data = (U16Vec) data();
   *   var valuesBox = new ValuesBox();
   *   getValues(index, valuesBox);
   *   return valuesBox.getTextWrap(data);
   * </pre>
   *
   * @param index the index of the String value
   * @return a TextWrap representing the list of U16 values or {@code null}
   * @throws IllegalStateException if the data is not a {@link U16Vec}
   *
   * @see TextWrap
   * @see #getString(long)
   */
  TextWrap getTextWrap(long index);

  /**
   * Returns the String decoded from a list of U16 values,
   * if the list is not valid, {@code null} is returned.
   *
   * Convenient method equivalent to
   * <pre>
   *   var textWrap = getTextWrap();
   *   return textWrap == null? null: textWrap.toString();
   * </pre>
   *
   * @param index the index of the String value
   * @return the String decoded from a list of U16 values or {@code null}
   * @throws IllegalStateException if the data is not a {@link U16Vec}
   * 
   * @see #getTextWrap(long)
   */
  String getString(long index);

  /**
   * Send the {@code validity} and the offset start and offset end of the list of vlaues to the {@code extractor}
   * @param index the index of the value
   * @see ValuesBox
   */
  void getValues(long index, ValuesExtractor extractor);

  @Override
  ListVec<V> withValidity(U1Vec validity);

  /**
   * Returns a Stream of all the list of U16 values as {@link TextWrap}s.
   * All the invalid values (the one with a {@code validity} equals to false) are encoded as {@code null}.
   *
   * @return a Stream of all the strings as {@link TextWrap}
   *
   * @see #getTextWrap(long)
   */
  Stream<TextWrap> allTextWraps();

  /**
   * A builder of {@link ListVec}
   *
   * This builder relies on 3 sub-builder, one for the validity bit set, one for the offset and
   * one for the data itself.
   *
   * Example
   * <pre>
   *   var dataPath = dir.resolve("data");
   *   var offsetPath = dir.resolve("offset");
   *   var validityPath = dir.resolve("validity");
   *
   *   ListVec<U8Vec> vec;   // each value is a list of U8
   *   try(var validityBuilder = U1Vec.builder(null, validityPath);
   *       var offsetBuilder = U32Vec.builder(null, offsetPath);
   *       var dataBuilder = U8Vec.builder(null, dataPath);
   *       var builder = ListVec.builder(validityBuilder, offsetBuilder, dataBuilder)) {
   *     LongStream.range(0, 100_000).forEach(i -> {
   *       builder.appendValues(b -> {   // append the list of values
   *         b.appendByte((byte) (i % 10))
   *          .appendByte((byte) -5);
   *       });
   *     });
   *     vec = builder.toVec();
   *   }
   *   try (vec) {
   *     assertEquals(100_000, vec.length());
   *
   *     var data = vec.data();
   *     var box = new ValuesBox();
   *     vec.getValues(6, box);   // extract the validity, startOffset and endOffset
   *     assertEquals(6, data.getByte(box.startOffset));        // first item
   *     assertEquals(-5, data.getByte(box.startOffset + 1));   // second item
   *
   *     vec.setNull(13);
   *     assertTrue(vec.isNull(13));
   *   }
   * </pre>
   *
   * To track null values, this Vec must have a {code validity} {@link U1Vec bit set}
   * either taken at construction or provided using {@link #withValidity(U1Vec)}.
   *
   * @see #builder(U1Vec.Builder, U32Vec.Builder, BaseBuilder)
   */
  interface Builder<D extends Vec, B extends BaseBuilder<D>> extends BaseBuilder<ListVec<D>> {
    B dataBuilder();

    /**
     * Appends a list of values to the data file with the corresponding offset to the offset file
     * @param consumer a consumer that give access to the data builder
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    ListVec.Builder<D, B> appendValues(Consumer<? super B> consumer) throws UncheckedIOException;

    @Override
    ListVec.Builder<D, B> appendNull() throws UncheckedIOException;

    ListVec.Builder<D, B> appendTextWrap(TextWrap textWrap) throws UncheckedIOException;

    ListVec.Builder<D, B> appendString(String s) throws UncheckedIOException;

    @Override
    ListVec<D> toVec();
  }

  /**
   * Creates a ListVec from an optional {@code validity} Vec, an {@code offset} Vec and a {@code data} Vec
   * @param validity a validity bitset or {@code null}
   * @param offset an offset Vec
   * @param data a data Vec
   * @param <V> the type of the data Vec
   * @return a newly created ListVec
   */
  static <V extends Vec> ListVec<V> from(U1Vec validity, U32Vec offset, V data) {
    requireNonNull(offset);
    requireNonNull(data);
    if (offset.length() <= 1) {
      throw new IllegalArgumentException("offsetSegment.length is too small");
    }
    if (validity != null && (offset.length() - 1) > validity.length()) {
      throw new IllegalArgumentException("validitySegment.length is too small");
    }
    return new VecImpl.ListImpl<>(data, impl(data).dataSegment(), impl(offset).dataSegment(), implDataOrNull(validity));
  }

  /**
   * Create a Vec builder that will append lists of values to the data builder and the offsets of those lists
   * to the offset builder.
   *
   * @param validityBuilder a builder able to create the validity bit set or {@code null}
   * @param offsetBuilder a builder able to create the offset file
   * @param dataBuilder a builder able to create the data file
   * @return a Vec builder that will append the list of values to a file before creating a Vec on those list of values
   */
  static <D extends Vec, B extends BaseBuilder<D>> ListVec.Builder<D, B> builder(U1Vec.Builder validityBuilder, U32Vec.Builder offsetBuilder, B dataBuilder) {
    requireNonNull(dataBuilder);
    requireNonNull(offsetBuilder);
    return new VecBuilderImpl.ListBuilder<>(dataBuilder, builderImpl(offsetBuilder), builderImpl(validityBuilder));
  }
}