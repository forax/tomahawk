package com.github.forax.tomahawk;

import java.io.UncheckedIOException;

interface UncheckedCloseable extends AutoCloseable {
  @Override
  void close() throws UncheckedIOException;
}