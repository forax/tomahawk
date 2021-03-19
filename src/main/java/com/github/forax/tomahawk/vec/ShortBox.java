package com.github.forax.tomahawk.vec;

public class ShortBox implements ShortExtractor {
  public boolean validity;
  public short value;

  @Override
  public void consume(boolean validity, short value) {
    this.validity = validity;
    this.value = value;
  }
}