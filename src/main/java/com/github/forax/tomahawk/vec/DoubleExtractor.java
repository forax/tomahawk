package com.github.forax.tomahawk.vec;

@FunctionalInterface
public
interface DoubleExtractor {
  void consume(boolean validity, double value);
}