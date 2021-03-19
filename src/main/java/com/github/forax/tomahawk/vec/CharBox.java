package com.github.forax.tomahawk.vec;

public class CharBox implements CharExtractor {
  public boolean validity;
  public char value;

  @Override
  public void consume(boolean validity, char value) {
    this.validity = validity;
    this.value = value;
  }
}