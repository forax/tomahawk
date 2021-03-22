package com.github.forax.tomahawk.vec;

import java.io.UncheckedIOException;

/**
 * Base interface of all {0code Vec}, a column of values or a list/structure of column of values
 * for the most complex implementation.
 *
 * There are 3 kinds of Vec
 * <ul>
 *   <li>Primitive Vec, {@link U8Vec}, {@link U16Vec}, {@link U32Vec} and {@link U64Vec} that stores
 *       values respectively as 8 bits, 16 bits, 32 bits or 64 bits.
 *   <li>{@link ListVec} that stores a list of list of values
 *   <li>{@link StructVec} that stores a struct of several Vecs
 * </ul>
 *
 * All Vec stores the values in memory thus must be closed using {@link #close()} to de-allocate
 * the corresponding memory.
 *
 * All Vec can store nullable values, for that each of them has a {@code validity} bit set
 * of type {@link U1Vec} indicating if the value is valid or {@code null}.
 */
public interface Vec extends UncheckedCloseable {
  /**
   * return the number of values stored in this Vec.
   * @return the number of values stored in this Vec.
   */
  long length();

  /**
   * Returns true if the value at index {@code index} is {@code null}
   * @param index the index of the value
   * @return true if the value at index {@code index} is {@code null}
   */
  boolean isNull(long index);

  /**
   * Se the value at index {@code index} to {@code null}
   * @param index the index of the value
   * @throws IllegalStateException if the Vec doesn't support null values (has no validity bit set)
   */
  void setNull(long index);

  /**
   * Creates a new Vec on the same memory zone with a new validity bit set
   * @param validity a validity bit set
   * @return a new Vec on the same memory zone with a new validity bit set
   */
  Vec withValidity(U1Vec validity);

  /**
   * Base interface for all builders of Vecs
   * @param <V> type of Vec to create
   */
  interface BaseBuilder<V extends Vec> extends UncheckedCloseable {
    /**
     * Returns the number of values already appended y this current builder.
     * @return the number of values already appended y this current builder.
     */
    long length();

    /**
     * Appends a {@code null} value to the file that will be mapped to a Vec
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the builder has no validity Vec
     */
    Vec.BaseBuilder<V> appendNull() throws UncheckedIOException;

    /**
     * Returns a new Vec that contains all the values appended by this builder
     * This method also {@link #close()} the current builder
     *
     * @returna new Vec that contains all the values appended by this builder
     * @throws UncheckedIOException if an IO error occurs
     *
     * @see #close()
     */
    V toVec() throws UncheckedIOException;
  }
}