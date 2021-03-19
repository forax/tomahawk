package com.github.forax.tomahawk.vec;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import jdk.incubator.foreign.MemorySegment;

public interface U16Vec extends Vec {
  short getShort(long index);
  void setShort(long index, short value);
  char getChar(long index);
  void setChar(long index, char value);
  void getShort(long index, ShortExtractor extractor);
  void getChar(long index, CharExtractor extractor);
  @Override
  U16Vec withValidity(U1Vec validity);

  interface Builder extends BaseBuilder<U16Vec> {
    U16Vec.Builder appendShort(short value) throws UncheckedIOException;
    U16Vec.Builder appendChar(char value) throws UncheckedIOException;
    @Override
    U16Vec.Builder appendNull() throws UncheckedIOException;

    U16Vec.Builder appendShortArray(short... array) throws UncheckedIOException;
    U16Vec.Builder appendCharArray(char... array) throws UncheckedIOException;
    U16Vec.Builder appendString(String s) throws UncheckedIOException;

    @Override
    U16Vec toVec();
  }

  static U16Vec wrap(short[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }
  static U16Vec wrap(char[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(memorySegment, null);
  }

  static U16Vec map(Path path, U1Vec validity) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
    return from(memorySegment, validity);
  }

  static U16Vec from(MemorySegment data, U1Vec validity) {
    requireNonNull(data);
    return new VecImpl.U16Impl(data, implDataOrNull(validity));
  }

  static U16Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U16Builder(path, output, builderImpl(validityBuilder));
  }
}