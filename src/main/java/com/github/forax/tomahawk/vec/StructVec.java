package com.github.forax.tomahawk.vec;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.impl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;

/**
 * A fixed size, mutable struct of columns
 *
 * It can be created
 * <ul>
 *   <li>Using a builder creates from an array of builders {@link #builder(U1Vec.Builder, BaseBuilder[])}
 *   <li>From an array of Vecs {@link #from(U1Vec, Vec...)}
 * </ul>
 *
 * Example
 * <pre>
 *   var offsetNamePath = dir.resolve("offset-name");
 *   var namePath = dir.resolve("name");
 *   var salaryPath = dir.resolve("salary");
 *   var validityPath = dir.resolve("validity");
 *
 *   StructVec vec;
 *   try(var validityBuilder = U1Vec.builder(null, validityPath);
 *     var name = ListVec.builder(null,                              // a list of list of U16
 *         U32Vec.builder(null, offsetNamePath),
 *         U16Vec.builder(null, namePath));
 *     var salary = U32Vec.builder(null, salaryPath);                // a list of U32
 *     var builder = StructVec.builder(validityBuilder, name, salary)) {
 *     IntStream.range(0, 100_000).forEach(i -> {
 *       builder.appendRow(rowBuilder -> {
 *         rowBuilder.appendString(name, "person" + i)
 *                   .appendInt(salary, 1_000 + i % 100);
 *       });
 *     });
 *     vec = builder.toVec();
 *   }
 *   try (vec) {
 *     assertEquals(100_000, vec.length());
 *
 *     var name = (ListVec<?>) vec.fields().get(0);
 *     var salary = (U32Vec) vec.fields().get(1);
 *
 *     assertEquals("person757", name.getString(757));
 *     assertEquals(1_057, salary.getInt(757));
 *
 *     vec.setNull(13);
 *     assertTrue(vec.isNull(13));
 *   }
 * </pre>
 *
 * To track null values, this Vec must have a {code validity} {@link U1Vec bit set}
 * either taken at construction or provided using {@link #withValidity(U1Vec)}.
 */
public interface StructVec extends Vec {
  /**
   * Returns a list of sub-Vec, one per field of the structure
   * @return a list of sub-Vec, one per field of the structure
   */
  List<Vec> fields();

  @Override
  StructVec withValidity(U1Vec validity);

  /**
   * Creates a new StructVec with the fields from [{@code removeStart}, {@code removeEnd}[ removed
   * and the {@code vecs} being added instead.
   *
   * @param removeStart index of the first fields being removed
   * @param removeEnd index (not included) of the last fields being removed
   * @param vecs the Vecs to add
   * @return a new StructVec
   */
  StructVec splice(int removeStart, int removeEnd, Vec... vecs);

  interface Builder extends BaseBuilder<StructVec> {
    /**
     * Returns the list of all builders, one per field of the structure
     * @return the list of all builders, one per field of the structure
     * @see #addFieldBuilder(BaseBuilder)
     */
    List<BaseBuilder<?>> fieldBuilders();

    /**
     * Dynamically add a new field builder as the last column builder.
     * If the builder already contains values for the other column, the method {@link BaseBuilder#appendNull()}
     * is called enough time so each field builder has the same number of values.
     *
     * @param fieldBuilder the field builder to add as last column
     */
    void addFieldBuilder(BaseBuilder<?> fieldBuilder);

    /**
     * Append a row of values.
     * If some values of the fields are not fill, this builder will automatically add null to those columns.
     *
     * @param consumer a consumer that give access to the row builder
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    StructVec.Builder appendRow(Consumer<? super StructVec.RowBuilder> consumer) throws UncheckedIOException;

    @Override
    StructVec.Builder appendNull() throws UncheckedIOException;

    @Override
    StructVec toVec();
  }

  /**
   * Build one row of the StructVec
   */
  interface RowBuilder {
    /**
     * Appends null to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendNull(BaseBuilder<?> field) throws UncheckedIOException;

    /**
     * Appends a boolean to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendBoolean(U1Vec.Builder field, boolean value) throws UncheckedIOException;

    /**
     * Appends a byte to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendByte(U8Vec.Builder field, byte value) throws UncheckedIOException;

    /**
     * Appends a short to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendShort(U16Vec.Builder field, short value) throws UncheckedIOException;

    /**
     * Appends a char to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendChar(U16Vec.Builder field, char value) throws UncheckedIOException;

    /**
     * Appends an int to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendInt(U32Vec.Builder field, int value) throws UncheckedIOException;

    /**
     * Appends a float to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendFloat(U32Vec.Builder field, float value) throws UncheckedIOException;

    /**
     * Appends a long to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendLong(U64Vec.Builder field, long value) throws UncheckedIOException;

    /**
     * Appends a double to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendDouble(U64Vec.Builder field, double value) throws UncheckedIOException;

    /**
     * Appends the content of a {@link TextWrap} to the field builder and
     * records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the content to add to the builder
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendTextWrap(ListVec.Builder<U16Vec, U16Vec.Builder> field, TextWrap value) throws UncheckedIOException;

    /**
     * Appends a String to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param value the value to append
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     * 
     * @see #appendTextWrap(ListVec.Builder, TextWrap)
     */
    StructVec.RowBuilder appendString(ListVec.Builder<U16Vec, U16Vec.Builder> field, String value) throws UncheckedIOException;

    /**
     * Appends a list of values to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param consumer a consumer that makes the builder of the element of the list available
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    <D extends Vec, B extends BaseBuilder<D>> StructVec.RowBuilder appendValues(ListVec.Builder<D, B> field, Consumer<? super B> consumer) throws UncheckedIOException;

    /**
     * Appends a row to the field builder and records that the value for this field is filled.
     *
     * @param field the field builder for a column
     * @param consumer a consumer that makes the field builder of the struct available
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     * @throws IllegalStateException if the column value was already added
     */
    StructVec.RowBuilder appendRow(StructVec.Builder field, Consumer<? super StructVec.RowBuilder> consumer) throws UncheckedIOException;
  }

  /**
   * Creates a StructVec from an optional {@code validity} Vec and several sub-Vecs
   * @param validity a validity bitset or {@code null}
   * @param fields an array of sub-Vecs that compose the structure
   * @return a new StructVec
   */
  static StructVec from(U1Vec validity, Vec... fields) {
    return from(validity, List.of(fields));
  }

  /**
   * Creates a StructVec from an optional {@code validity} Vec and several sub-Vecs
   * @param validity a validity bitset or {@code null}
   * @param fields a list of sub-Vecs that compose the structure
   * @return a new StructVec
   */
  static StructVec from(U1Vec validity, List<? extends Vec> fields) {
    if (validity != null  && impl(validity).validitySegment() != null) {
      throw new IllegalArgumentException("validity can not have itself a validity vec");
    }
    return new VecImpl.StructImpl(implDataOrNull(validity), List.copyOf(fields));
  }

  /**
   * Create a Vec builder that will append rows of values to create a StructVec
   * to the offset builder.
   *
   * @param validityBuilder a builder able to create the validity bit set or {@code null}
   * @param fieldBuilders an array of sub-builders to create each column Vec
   * @return a Vec builder that will append rows of values to create a StructVec
   */
  static StructVec.Builder builder(U1Vec.Builder validityBuilder, BaseBuilder<?>... fieldBuilders) {
    return builder(validityBuilder, List.of(fieldBuilders));
  }

  /**
   * Create a Vec builder that will append rows of values to create a StructVec
   * to the offset builder.
   *
   * @param validityBuilder a builder able to create the validity bit set or {@code null}
   * @param fieldBuilders a list of sub-builders to create each column Vec
   * @return a Vec builder that will append rows of values to create a StructVec
   */
  static StructVec.Builder builder(U1Vec.Builder validityBuilder, List<? extends BaseBuilder<?>> fieldBuilders) {
    var builder = new VecBuilderImpl.StructBuilder(builderImpl(validityBuilder));
    for(var fieldBuilder: fieldBuilders) {
      builder.addFieldBuilder(fieldBuilder);
    }
    return builder;
  }
}