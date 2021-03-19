package com.github.forax.tomahawk.vec;

public class ByteBox implements ByteExtractor {
  public boolean validity;
  public byte value;

  @Override
  public void consume(boolean validity, byte value) {
    this.validity = validity;
    this.value = value;
  }
}