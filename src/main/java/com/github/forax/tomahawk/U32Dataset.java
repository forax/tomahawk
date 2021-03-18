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

public interface U32Dataset extends Dataset {
  int getInt(long index);
  void setInt(long index, int value);
  float getFloat(long index);
  void setFloat(long index, float value);
  void getInt(long index, IntExtractor extractor);
  void getFloat(long index, FloatExtractor extractor);
  @Override
  U32Dataset withValidity(U1Dataset validity);

  interface Builder extends BaseBuilder<U32Dataset> {
    U32Dataset.Builder appendInt(int value) throws UncheckedIOException;
    U32Dataset.Builder appendFloat(float value) throws UncheckedIOException;
    @Override
    U32Dataset.Builder appendNull() throws UncheckedIOException;

    @Override
    U32Dataset toDataset() throws UncheckedIOException;
  }

  static U32Dataset wrap(int[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }
  static U32Dataset wrap(float[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }

  static U32Dataset map(Path path, U1Dataset validity) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(memorySegment, validity);
  }

  static U32Dataset from(MemorySegment data, U1Dataset validity) {
    requireNonNull(data);
    return new DatasetImpl.U32Impl(data, implDataOrNull(validity));
  }

  static U32Dataset.Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new DatasetBuilderImpl.U32Builder(path, output, builderImpl(validityBuilder));
  }
}