package com.github.forax.tomahawk;

import static com.github.forax.tomahawk.DatasetBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.DatasetImpl.impl;
import static com.github.forax.tomahawk.DatasetImpl.implDataOrNull;
import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.util.function.Consumer;

public interface ListDataset<D extends Dataset> extends Dataset {
  D data();
  String getString(long index);
  void getValues(long index, ValuesExtractor extractor);
  @Override
  ListDataset<D> withValidity(U1Dataset validity);

  interface Builder<D extends Dataset, B extends BaseBuilder<D>> extends BaseBuilder<ListDataset<D>> {
    ListDataset.Builder<D, B> appendValues(Consumer<? super B> consumer) throws UncheckedIOException;
    @Override
    ListDataset.Builder<D, B> appendNull() throws UncheckedIOException;

    ListDataset.Builder<D, B> appendString(String s) throws UncheckedIOException;

    @Override
    ListDataset<D> toDataset();
  }

  static <D extends Dataset> ListDataset<D> from(D data, U32Dataset offset, U1Dataset validity) throws UncheckedIOException {
    if (offset.length() <= 1) {
      throw new IllegalArgumentException("offsetSegment.length is too small");
    }
    if (validity != null && (offset.length() - 1) > validity.length()) {
      throw new IllegalArgumentException("validitySegment.length is too small");
    }
    return new DatasetImpl.ListImpl<>(data, impl(data).dataSegment(), impl(offset).dataSegment(), implDataOrNull(validity));
  }

  static <D extends Dataset, B extends BaseBuilder<D>> ListDataset.Builder<D, B> builder(B dataBuilder, U32Dataset.Builder offsetBuilder, U1Dataset.Builder validityBuilder) {
    requireNonNull(dataBuilder);
    requireNonNull(offsetBuilder);
    return new DatasetBuilderImpl.ListBuilder<>(dataBuilder, builderImpl(offsetBuilder), builderImpl(validityBuilder));
  }
}