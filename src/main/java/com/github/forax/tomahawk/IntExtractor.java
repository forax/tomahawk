package com.github.forax.tomahawk;

@FunctionalInterface
public
interface IntExtractor {
  void consume(boolean validity, int value);
}