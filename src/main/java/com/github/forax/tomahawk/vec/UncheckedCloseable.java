package com.github.forax.tomahawk.vec;

import java.io.UncheckedIOException;

/**
 * Close the Vec or the Builder with tunneling all IO exceptions into {@link UncheckedIOException}.
 */
interface UncheckedCloseable extends AutoCloseable {
  /**
   * Close and de-allocate the corresponding resources.
   * This method is idempotent so can be called multiple times, after the first call,
   * all subsequent calls do nothing.
   *
   * @throws UncheckedIOException if an IO error occurs
   */
  @Override
  void close() throws UncheckedIOException;
}