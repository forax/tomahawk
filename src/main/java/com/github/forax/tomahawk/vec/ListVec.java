package com.github.forax.tomahawk.vec;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.impl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.util.function.Consumer;

public interface ListVec<D extends Vec> extends Vec {
  D data();
  String getString(long index);
  void getValues(long index, ValuesExtractor extractor);
  @Override
  ListVec<D> withValidity(U1Vec validity);

  interface Builder<D extends Vec, B extends BaseBuilder<D>> extends BaseBuilder<ListVec<D>> {
    ListVec.Builder<D, B> appendValues(Consumer<? super B> consumer) throws UncheckedIOException;
    @Override
    ListVec.Builder<D, B> appendNull() throws UncheckedIOException;

    ListVec.Builder<D, B> appendString(String s) throws UncheckedIOException;

    @Override
    ListVec<D> toVec();
  }

  static <D extends Vec> ListVec<D> from(D data, U32Vec offset, U1Vec validity) throws UncheckedIOException {
    if (offset.length() <= 1) {
      throw new IllegalArgumentException("offsetSegment.length is too small");
    }
    if (validity != null && (offset.length() - 1) > validity.length()) {
      throw new IllegalArgumentException("validitySegment.length is too small");
    }
    return new VecImpl.ListImpl<>(data, impl(data).dataSegment(), impl(offset).dataSegment(), implDataOrNull(validity));
  }

  static <D extends Vec, B extends BaseBuilder<D>> ListVec.Builder<D, B> builder(B dataBuilder, U32Vec.Builder offsetBuilder, U1Vec.Builder validityBuilder) {
    requireNonNull(dataBuilder);
    requireNonNull(offsetBuilder);
    return new VecBuilderImpl.ListBuilder<>(dataBuilder, builderImpl(offsetBuilder), builderImpl(validityBuilder));
  }
}