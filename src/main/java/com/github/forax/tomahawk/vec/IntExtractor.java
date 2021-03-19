package com.github.forax.tomahawk.vec;

@FunctionalInterface
public
interface IntExtractor {
  void consume(boolean validity, int value);
}