package com.github.forax.tomahawk.vec;

public class LongBox implements LongExtractor {
  public boolean validity;
  public long value;

  @Override
  public void consume(boolean validity, long value) {
    this.validity = validity;
    this.value = value;
  }
}