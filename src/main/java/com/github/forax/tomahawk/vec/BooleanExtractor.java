package com.github.forax.tomahawk.vec;

@FunctionalInterface
public
interface BooleanExtractor {
  void consume(boolean validity, boolean value);
}