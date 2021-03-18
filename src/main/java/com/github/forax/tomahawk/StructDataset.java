package com.github.forax.tomahawk;

import static com.github.forax.tomahawk.DatasetBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.DatasetImpl.implDataOrNull;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;

public interface StructDataset extends Dataset {
  List<Dataset> fields();

  @Override
  StructDataset withValidity(U1Dataset validity);

  interface Builder extends BaseBuilder<StructDataset> {
    void attachField(BaseBuilder<?> fieldBuilder);
    StructDataset.Builder appendRow(Consumer<? super StructDataset.RowBuilder> consumer) throws UncheckedIOException;
    @Override
    StructDataset.Builder appendNull() throws UncheckedIOException;

    @Override
    StructDataset toDataset();
  }

  interface RowBuilder {
    StructDataset.RowBuilder appendNull(BaseBuilder<?> field) throws UncheckedIOException;
    StructDataset.RowBuilder appendShort(U16Dataset.Builder field, short value) throws UncheckedIOException;
    StructDataset.RowBuilder appendChar(U16Dataset.Builder field, char value) throws UncheckedIOException;
    StructDataset.RowBuilder appendInt(U32Dataset.Builder field, int value) throws UncheckedIOException;
    StructDataset.RowBuilder appendFloat(U32Dataset.Builder field, float value) throws UncheckedIOException;
    <D extends Dataset, B extends BaseBuilder<D>> StructDataset.RowBuilder appendValues(ListDataset.Builder<D, B> field, Consumer<? super B> consumer) throws UncheckedIOException;
    StructDataset.RowBuilder appendString(ListDataset.Builder<U16Dataset, U16Dataset.Builder> field, String s) throws UncheckedIOException;
  }

  static StructDataset from(U1Dataset validity, List<Dataset> fields) throws UncheckedIOException {
    return new DatasetImpl.StructImpl(implDataOrNull(validity), fields);
  }

  static StructDataset.Builder builder(U1Dataset.Builder validityBuilder, BaseBuilder<?>... fieldBuilders) {
     var builder = new DatasetBuilderImpl.StructBuilder(builderImpl(validityBuilder));
     for(var fieldBuilder: fieldBuilders) {
       builder.attachField(fieldBuilder);
     }
     return builder;
  }
}