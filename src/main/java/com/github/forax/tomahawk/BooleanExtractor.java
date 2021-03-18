package com.github.forax.tomahawk;

@FunctionalInterface
public
interface BooleanExtractor {
  void consume(boolean validity, boolean value);
}