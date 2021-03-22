package com.github.forax.tomahawk.vec;

/**
 * Function with a nullable boolean value as parameter.
 *
 * @see U1Vec#getBoolean(long, BooleanExtractor)
 * @see BooleanBox
 */
@FunctionalInterface
public interface BooleanExtractor {
  /**
   * Called with a nullable value, if {@code validity} is false, then the corresponding value
   * is {@code null} otherwise the value is {@code value}.
   *
   * @param validity {@code false} if the value is {@code null}
   * @param value the value if the {@code validity} is {@code true}
   */
  void consume(boolean validity, boolean value);
}