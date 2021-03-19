package com.github.forax.tomahawk.vec;

public class FloatBox implements FloatExtractor {
  public boolean validity;
  public float value;

  @Override
  public void consume(boolean validity, float value) {
    this.validity = validity;
    this.value = value;
  }
}