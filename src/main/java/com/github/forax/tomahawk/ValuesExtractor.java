package com.github.forax.tomahawk;

public interface ValuesExtractor {
  void consume(boolean validity, long startOffset, long endOffset);
}