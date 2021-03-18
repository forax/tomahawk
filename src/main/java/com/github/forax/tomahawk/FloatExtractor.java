package com.github.forax.tomahawk;

@FunctionalInterface
public
interface FloatExtractor {
  void consume(boolean validity, float value);
}