package com.github.forax.tomahawk;

import java.io.UncheckedIOException;

public interface Vec extends UncheckedCloseable {
  long length();
  boolean isNull(long index);
  void setNull(long index);
  Vec withValidity(U1Vec validity);

  interface BaseBuilder<D extends Vec> extends UncheckedCloseable {
    long length();
    Vec.BaseBuilder<D> appendNull() throws UncheckedIOException;
    D toVec() throws UncheckedIOException;
  }
}