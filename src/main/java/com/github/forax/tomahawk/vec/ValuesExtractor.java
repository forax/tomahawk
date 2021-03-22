package com.github.forax.tomahawk.vec;

/**
 * Function with a nullable interval of values as parameter.
 *
 * @see ListVec#getValues(long, ValuesExtractor)
 * @see ValuesBox
 */
@FunctionalInterface
public interface ValuesExtractor {
  /**
   * Called with a nullable list of values, if {@code validity} is false, then the corresponding list of values
   * is {@code null} otherwise the values are in between {@code startOffset} and {@code #endOffset}.
   *
   * @param validity {@code false} if the value is {@code null}
   * @param startOffset start offset of the values if {@code validity} is true
   * @param endOffset end offset (excluded) of the values if {@code validity} is true
   */
  void consume(boolean validity, long startOffset, long endOffset);
}