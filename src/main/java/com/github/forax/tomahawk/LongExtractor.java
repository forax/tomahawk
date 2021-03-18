package com.github.forax.tomahawk;

@FunctionalInterface
public
interface LongExtractor {
  void consume(boolean validity, long value);
}