package com.github.forax.tomahawk.vec;

public interface ValuesExtractor {
  void consume(boolean validity, long startOffset, long endOffset);
}