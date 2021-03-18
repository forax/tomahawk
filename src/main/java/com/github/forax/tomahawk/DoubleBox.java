package com.github.forax.tomahawk;

public class DoubleBox implements DoubleExtractor {
  public boolean validity;
  public double value;

  @Override
  public void consume(boolean validity, double value) {
    this.validity = validity;
    this.value = value;
  }
}