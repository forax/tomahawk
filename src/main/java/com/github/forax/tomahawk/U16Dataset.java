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

public interface U16Dataset extends Dataset {
  short getShort(long index);
  void setShort(long index, short value);
  char getChar(long index);
  void setChar(long index, char value);
  void getShort(long index, ShortExtractor extractor);
  void getChar(long index, CharExtractor extractor);
  @Override
  U16Dataset withValidity(U1Dataset validity);

  interface Builder extends BaseBuilder<U16Dataset> {
    U16Dataset.Builder appendShort(short value) throws UncheckedIOException;
    U16Dataset.Builder appendChar(char value) throws UncheckedIOException;
    @Override
    U16Dataset.Builder appendNull() throws UncheckedIOException;

    U16Dataset.Builder appendShortArray(short... array) throws UncheckedIOException;
    U16Dataset.Builder appendCharArray(char... array) throws UncheckedIOException;
    U16Dataset.Builder appendString(String s) throws UncheckedIOException;

    @Override
    U16Dataset toDataset();
  }

  static U16Dataset wrap(short[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }
  static U16Dataset wrap(char[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }

  static U16Dataset map(Path path, U1Dataset validity) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(memorySegment, validity);
  }

  static U16Dataset from(MemorySegment data, U1Dataset validity) {
    requireNonNull(data);
    return new DatasetImpl.U16Impl(data, implDataOrNull(validity));
  }

  static U16Dataset.Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new DatasetBuilderImpl.U16Builder(path, output, builderImpl(validityBuilder));
  }
}