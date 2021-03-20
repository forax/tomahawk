package com.github.forax.tomahawk.vec;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import jdk.incubator.foreign.MemorySegment;

public interface U1Vec extends Vec {
  boolean getBoolean(long index);
  void setBoolean(long index, boolean value);
  void getBoolean(long index, BooleanExtractor extractor);

  @Override
  U1Vec withValidity(U1Vec validity);

  interface Builder extends BaseBuilder<U1Vec> {
    U1Vec.Builder appendBoolean(boolean value);
    @Override
    U1Vec.Builder appendNull();

    @Override
    U1Vec toVec();
  }

  static U1Vec wrap(long[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  static U1Vec map(U1Vec validity, Path path) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(validity, memorySegment);
  }

  static U1Vec from(U1Vec validity, MemorySegment memorySegment) {
    requireNonNull(memorySegment);
    return new VecImpl.U1Impl(memorySegment, implDataOrNull(validity));
  }

  static U1Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U1Builder(path, output, builderImpl(validityBuilder));
  }
}