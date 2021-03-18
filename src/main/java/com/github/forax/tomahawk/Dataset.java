package com.github.forax.tomahawk;

import java.io.UncheckedIOException;

public interface Dataset extends UncheckedCloseable {
  long length();
  boolean isNull(long index);
  void setNull(long index);
  Dataset withValidity(U1Dataset validity);

  interface BaseBuilder<D extends Dataset> extends UncheckedCloseable {
    long length();
    Dataset.BaseBuilder<D> appendNull() throws UncheckedIOException;
    D toDataset() throws UncheckedIOException;
  }
}