package com.github.forax.tomahawk.vec;

@FunctionalInterface
public
interface FloatExtractor {
  void consume(boolean validity, float value);
}