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
   * Cast the current Vec as a {@link ListVec} of a given {@code elementType}.
   * @param elementType the element Vec of the {@code ListVec}
   * @param <V> the type of the element Vec
   * @return the current Vec typed as a {@code ListVec}
   * @throws ClassCastException if either the current Vec is not a ListVec or the ListVec
   *                            does not have the right element type
   */
  @SuppressWarnings("unchecked")
  default <V extends Vec> ListVec<V> asListOf(Class<? extends V> elementType) {
    @SuppressWarnings("RedundantCast")  // the error message should not mention the implementation
    var listVec = (VecImpl.ListImpl<?>) (ListVec<?>) this;
    var data = listVec.element();
    if (!elementType.isInstance(data)) {
      throw new ClassCastException("element vec is not a " + elementType.getName() + " but a " + data.getClass().getName());
    }
    return (ListVec<V>) listVec;
  }

  /**
   * Cast the current Vec as a {@link StructVec}
   * @return the current Vec typed as a {@link StructVec}
   * @throws ClassCastException if the current Vec is not a StructVec
   */
  default StructVec asStruct() {
    return (StructVec) this;
  }

  /**
   * Cast the current Vec as a specific Vec
   * @param vecType the type of Vec to be cast to
   * @param <V> type of the specific Vec
   * @return the current Vec typed as a specific Vec
   * @throws ClassCastException if the current Vec is not a specific Vec
   */
  default <V extends Vec> V as(Class<? extends V> vecType) {
    return vecType.cast(this);
  }

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