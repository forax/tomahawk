package com.github.forax.tomahawk;

import static com.github.forax.tomahawk.DatasetBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.DatasetImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import jdk.incubator.foreign.MemorySegment;

public interface U1Dataset extends Dataset {
  boolean getBoolean(long index);
  void setBoolean(long index, boolean value);
  void getBoolean(long index, BooleanExtractor extractor);

  @Override
  U1Dataset withValidity(U1Dataset validity);

  interface Builder extends BaseBuilder<U1Dataset> {
    U1Dataset.Builder appendBoolean(boolean value);
    @Override
    U1Dataset.Builder appendNull();

    @Override
    U1Dataset toDataset();
  }

  static U1Dataset wrap(long[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }

  static U1Dataset map(Path path, U1Dataset validity) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(memorySegment, validity);
  }

  static U1Dataset from(MemorySegment memorySegment, U1Dataset validity) {
    requireNonNull(memorySegment);
    return new DatasetImpl.U1Impl(memorySegment, implDataOrNull(validity));
  }

  static U1Dataset.Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new DatasetBuilderImpl.U1Builder(path, output, builderImpl(validityBuilder));
  }
}