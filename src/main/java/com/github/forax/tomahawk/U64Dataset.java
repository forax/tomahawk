package com.github.forax.tomahawk;

import static com.github.forax.tomahawk.DatasetBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.DatasetImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import jdk.incubator.foreign.MemorySegment;

public interface U64Dataset extends Dataset {
  long getLong(long index);
  void setLong(long index, long value);
  double getDouble(long index);
  void setDouble(long index, double value);
  void getLong(long index, LongExtractor extractor);
  void getDouble(long index, DoubleExtractor extractor);

  @Override
  U64Dataset withValidity(U1Dataset validity);

  interface Builder extends BaseBuilder<U64Dataset> {
    U64Dataset.Builder appendLong(long value) throws UncheckedIOException;
    U64Dataset.Builder appendDouble(double value) throws UncheckedIOException;
    @Override
    U64Dataset.Builder appendNull() throws UncheckedIOException;

    @Override
    U64Dataset toDataset();
  }

  static U64Dataset wrap(long[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }
  static U64Dataset wrap(double[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }

  static U64Dataset map(Path path, U1Dataset validity) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(memorySegment, validity);
  }

  static U64Dataset from(MemorySegment data, U1Dataset validity) {
    requireNonNull(data);
    return new DatasetImpl.U64Impl(data, implDataOrNull(validity));
  }

  static U64Dataset.Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new DatasetBuilderImpl.U64Builder(path, output, builderImpl(validityBuilder));
  }
}