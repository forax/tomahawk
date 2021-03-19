package com.github.forax.tomahawk.vec;

import java.io.UncheckedIOException;

interface UncheckedCloseable extends AutoCloseable {
  @Override
  void close() throws UncheckedIOException;
}