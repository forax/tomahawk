package com.github.forax.tomahawk.vec;

import jdk.incubator.foreign.MemorySegment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.util.Objects.requireNonNull;

public interface U64Vec extends Vec {
  long getLong(long index);
  void setLong(long index, long value);
  double getDouble(long index);
  void setDouble(long index, double value);
  void getLong(long index, LongExtractor extractor);
  void getDouble(long index, DoubleExtractor extractor);

  @Override
  U64Vec withValidity(U1Vec validity);

  interface Builder extends BaseBuilder<U64Vec> {
    U64Vec.Builder appendLong(long value) throws UncheckedIOException;
    U64Vec.Builder appendDouble(double value) throws UncheckedIOException;
    @Override
    U64Vec.Builder appendNull() throws UncheckedIOException;

    @Override
    U64Vec toVec();
  }

  static U64Vec wrap(long[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }
  static U64Vec wrap(double[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  static U64Vec map(U1Vec validity, Path path) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_WRITE);
    return from(validity, memorySegment);
  }

  static U64Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length << 3);
    return map(validity, path);
  }

  static U64Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    return new VecImpl.U64Impl(data, implDataOrNull(validity));
  }

  static U64Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U64Builder(path, output, builderImpl(validityBuilder));
  }
}