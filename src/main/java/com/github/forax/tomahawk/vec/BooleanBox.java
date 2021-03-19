package com.github.forax.tomahawk.vec;

public class BooleanBox implements BooleanExtractor {
  public boolean validity;
  public boolean value;

  @Override
  public void consume(boolean validity, boolean value) {
    this.validity = validity;
    this.value = value;
  }
}