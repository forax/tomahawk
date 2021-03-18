package com.github.forax.tomahawk;

@FunctionalInterface
public
interface DoubleExtractor {
  void consume(boolean validity, double value);
}