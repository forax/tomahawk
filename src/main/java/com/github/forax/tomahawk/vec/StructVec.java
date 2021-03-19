package com.github.forax.tomahawk.vec;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

public interface StructVec extends Vec {
  List<Vec> fields();

  @Override
  StructVec withValidity(U1Vec validity);

  interface Builder extends BaseBuilder<StructVec> {
    void attachField(BaseBuilder<?> fieldBuilder);
    StructVec.Builder appendRow(Consumer<? super StructVec.RowBuilder> consumer) throws UncheckedIOException;
    @Override
    StructVec.Builder appendNull() throws UncheckedIOException;

    @Override
    StructVec toVec();
  }

  interface RowBuilder {
    StructVec.RowBuilder appendNull(BaseBuilder<?> field) throws UncheckedIOException;
    StructVec.RowBuilder appendShort(U16Vec.Builder field, short value) throws UncheckedIOException;
    StructVec.RowBuilder appendChar(U16Vec.Builder field, char value) throws UncheckedIOException;
    StructVec.RowBuilder appendInt(U32Vec.Builder field, int value) throws UncheckedIOException;
    StructVec.RowBuilder appendFloat(U32Vec.Builder field, float value) throws UncheckedIOException;
    <D extends Vec, B extends BaseBuilder<D>> StructVec.RowBuilder appendValues(ListVec.Builder<D, B> field, Consumer<? super B> consumer) throws UncheckedIOException;
    StructVec.RowBuilder appendString(ListVec.Builder<U16Vec, U16Vec.Builder> field, String s) throws UncheckedIOException;
  }

  static StructVec from(U1Vec validity, List<Vec> fields) throws UncheckedIOException {
    return new VecImpl.StructImpl(implDataOrNull(validity), fields);
  }

  static StructVec.Builder builder(U1Vec.Builder validityBuilder, BaseBuilder<?>... fieldBuilders) {
     var builder = new VecBuilderImpl.StructBuilder(builderImpl(validityBuilder));
     for(var fieldBuilder: fieldBuilders) {
       builder.attachField(fieldBuilder);
     }
     return builder;
  }
}