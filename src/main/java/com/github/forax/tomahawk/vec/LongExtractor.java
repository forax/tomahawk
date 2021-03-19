package com.github.forax.tomahawk.vec;

@FunctionalInterface
public
interface LongExtractor {
  void consume(boolean validity, long value);
}