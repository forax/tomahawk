package com.github.forax.tomahawk;

public class CharBox implements CharExtractor {
  public boolean validity;
  public char value;

  @Override
  public void consume(boolean validity, char value) {
    this.validity = validity;
    this.value = value;
  }
}