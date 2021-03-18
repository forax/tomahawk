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

public interface U8Dataset extends Dataset {
  byte getByte(long index);
  void setByte(long index, byte value);
  void getByte(long index, ByteExtractor extractor);
  @Override
  U8Dataset withValidity(U1Dataset validity);

  interface Builder extends BaseBuilder<U8Dataset> {
    U8Dataset.Builder appendByte(byte value) throws UncheckedIOException;
    @Override
    U8Dataset.Builder appendNull() throws UncheckedIOException;

    @Override
    U8Dataset toDataset();
  }

  static U8Dataset wrap(byte[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }

  static U8Dataset map(Path path, U1Dataset validity) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(memorySegment, validity);
  }

  static U8Dataset from(MemorySegment data, U1Dataset validity) {
    requireNonNull(data);
    return new DatasetImpl.U8Impl(data, implDataOrNull(validity));
  }

  static U8Dataset.Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new DatasetBuilderImpl.U8Builder(path, output, builderImpl(validityBuilder));
  }
}