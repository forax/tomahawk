package com.github.forax.tomahawk;

public class IntBox implements IntExtractor {
  public boolean validity;
  public int value;

  @Override
  public void consume(boolean validity, int value) {
    this.validity = validity;
    this.value = value;
  }
}