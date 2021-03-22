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

/**
 * A fixed size, mutable column of 16 bits values.
 *
 * It can be created from several ways
 * <ul>
 *   <li>By wrapping an array of Java shorts {@link #wrap(short[])}, or an array of Java chars {@link #wrap(char[])}
 *   <li>By mapping into memory an existing file {@link #map(U1Vec, Path)}
 *   <li>By mapping into memory a new empty file {@link #mapNew(U1Vec, Path, long)}
 *   <li>Using a builder {@link #builder(U1Vec.Builder, Path, OpenOption...)} to append values to a new mapped file
 *   <li>From an existing MemorySegment {@link #from(U1Vec, MemorySegment)}
 * </ul>
 *
 * It can load and store nulls, shorts and chars
 * <ul>
 *   <li>{@link #getShort(long)} and {@link #getChar(long)} loads a non null short / char
 *   <li>{@link #getShort(long, ShortExtractor)} and {@link #getChar(long, CharExtractor)}
 *   loads a nullable short / char
 *   <li>{@link #setShort(long, short)} and {@link #setChar(long, char)} stores an short / char
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * To store nulls, this Vec must be constructed with a {code validity} {@link U1Vec bit set} either at construction
 * or using {@link #withValidity(U1Vec)}.
 */
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
    return from(null, memorySegment);
  }
  static U16Vec wrap(char[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  static U16Vec map(U1Vec validity, Path path) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_WRITE);
    return from(validity, memorySegment);
  }

  static U16Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length << 1);
    return map(validity, path);
  }

  static U16Vec from(U1Vec validity, MemorySegment data) {
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