package com.github.forax.tomahawk.vec;

/**
 * A mutable class that represents a nullable char value.
 *
 * If {@link #validity} is true, the value of the box is stored into {@link #value},
 * otherwise if {@link #validity} is false, the value of the box is {@code null}.
 *
 * @see U16Vec#getChar(long, CharBox)
 */
public class CharBox {
  /**
   * The validity of the value, false means that the value doesn't exist (is null)
   */
  public boolean validity;

  /**
   * The value, if the validity is {@code false}, the value of {@code value} should
   * not taken into account.
   */
  public char value;

  void fill(boolean validity, char value) {
    this.validity = validity;
    this.value = value;
  }
}