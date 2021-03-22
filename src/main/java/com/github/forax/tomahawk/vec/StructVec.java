package com.github.forax.tomahawk.vec;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;

/**
 * A fixed size, mutable column of 32 bits values.
 *
 * It can be created from several ways
 * <ul>
 *   <li>Using a builder creates from an array of builders {@link #builder(U1Vec.Builder, BaseBuilder[])}
 *   <li>From an array of Vecs {@link #from(U1Vec, Vec...)}
 * </ul>
 *
 * To store nulls, this Vec must be constructed with a {code validity} {@link U1Vec bit set} either at construction
 * or using {@link #withValidity(U1Vec)}.
 */
public interface StructVec extends Vec {
  List<Vec> fields();

  @Override
  StructVec withValidity(U1Vec validity);

  interface Builder extends BaseBuilder<StructVec> {
    List<BaseBuilder<?>> fieldBuilders();
    void attachField(BaseBuilder<?> fieldBuilder);

    StructVec.Builder appendRow(Consumer<? super StructVec.RowBuilder> consumer) throws UncheckedIOException;
    @Override
    StructVec.Builder appendNull() throws UncheckedIOException;

    @Override
    StructVec toVec();
  }

  interface RowBuilder {
    StructVec.RowBuilder appendNull(BaseBuilder<?> field) throws UncheckedIOException;
    StructVec.RowBuilder appendBoolean(U1Vec.Builder field, boolean value) throws UncheckedIOException;
    StructVec.RowBuilder appendByte(U8Vec.Builder field, byte value) throws UncheckedIOException;
    StructVec.RowBuilder appendShort(U16Vec.Builder field, short value) throws UncheckedIOException;
    StructVec.RowBuilder appendChar(U16Vec.Builder field, char value) throws UncheckedIOException;
    StructVec.RowBuilder appendInt(U32Vec.Builder field, int value) throws UncheckedIOException;
    StructVec.RowBuilder appendFloat(U32Vec.Builder field, float value) throws UncheckedIOException;
    StructVec.RowBuilder appendLong(U64Vec.Builder field, long value) throws UncheckedIOException;
    StructVec.RowBuilder appendDouble(U64Vec.Builder field, double value) throws UncheckedIOException;
    <D extends Vec, B extends BaseBuilder<D>> StructVec.RowBuilder appendValues(ListVec.Builder<D, B> field, Consumer<? super B> consumer) throws UncheckedIOException;
    StructVec.RowBuilder appendString(ListVec.Builder<U16Vec, U16Vec.Builder> field, String s) throws UncheckedIOException;
  }

  static StructVec from(U1Vec validity, Vec... fields) throws UncheckedIOException {
    return from(validity, List.of(fields));
  }

  static StructVec from(U1Vec validity, List<? extends Vec> fields) throws UncheckedIOException {
    return new VecImpl.StructImpl(implDataOrNull(validity), List.copyOf(fields));
  }

  static StructVec.Builder builder(U1Vec.Builder validityBuilder, BaseBuilder<?>... fieldBuilders) {
    return builder(validityBuilder, List.of(fieldBuilders));
  }

  static StructVec.Builder builder(U1Vec.Builder validityBuilder, List<? extends BaseBuilder<?>> fieldBuilders) {
    var builder = new VecBuilderImpl.StructBuilder(builderImpl(validityBuilder));
    for(var fieldBuilder: fieldBuilders) {
      builder.attachField(fieldBuilder);
    }
    return builder;
  }
}