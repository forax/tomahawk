package com.github.forax.tomahawk;

import static com.github.forax.tomahawk.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import jdk.incubator.foreign.MemorySegment;

public interface U8Vec extends Vec {
  byte getByte(long index);
  void setByte(long index, byte value);
  void getByte(long index, ByteExtractor extractor);
  @Override
  U8Vec withValidity(U1Vec validity);

  interface Builder extends BaseBuilder<U8Vec> {
    U8Vec.Builder appendByte(byte value) throws UncheckedIOException;
    @Override
    U8Vec.Builder appendNull() throws UncheckedIOException;

    @Override
    U8Vec toVec();
  }

  static U8Vec wrap(byte[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }

  static U8Vec map(Path path, U1Vec validity) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(memorySegment, validity);
  }

  static U8Vec from(MemorySegment data, U1Vec validity) {
    requireNonNull(data);
    return new VecImpl.U8Impl(data, implDataOrNull(validity));
  }

  static U8Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U8Builder(path, output, builderImpl(validityBuilder));
  }
}