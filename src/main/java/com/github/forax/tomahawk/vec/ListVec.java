package com.github.forax.tomahawk.vec;

import java.io.UncheckedIOException;
import java.util.function.Consumer;

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
  V data();
  String getString(long index);
  void getValues(long index, ValuesExtractor extractor);
  @Override
  ListVec<V> withValidity(U1Vec validity);

  interface Builder<D extends Vec, B extends BaseBuilder<D>> extends BaseBuilder<ListVec<D>> {
    B dataBuilder();

    ListVec.Builder<D, B> appendValues(Consumer<? super B> consumer) throws UncheckedIOException;
    @Override
    ListVec.Builder<D, B> appendNull() throws UncheckedIOException;

    ListVec.Builder<D, B> appendString(String s) throws UncheckedIOException;

    @Override
    ListVec<D> toVec();
  }

  static <D extends Vec> ListVec<D> from(U1Vec validity, U32Vec offset, D data) throws UncheckedIOException {
    if (offset.length() <= 1) {
      throw new IllegalArgumentException("offsetSegment.length is too small");
    }
    if (validity != null && (offset.length() - 1) > validity.length()) {
      throw new IllegalArgumentException("validitySegment.length is too small");
    }
    return new VecImpl.ListImpl<>(data, impl(data).dataSegment(), impl(offset).dataSegment(), implDataOrNull(validity));
  }

  static <D extends Vec, B extends BaseBuilder<D>> ListVec.Builder<D, B> builder(U1Vec.Builder validityBuilder, U32Vec.Builder offsetBuilder, B dataBuilder) {
    requireNonNull(dataBuilder);
    requireNonNull(offsetBuilder);
    return new VecBuilderImpl.ListBuilder<>(dataBuilder, builderImpl(offsetBuilder), builderImpl(validityBuilder));
  }
}