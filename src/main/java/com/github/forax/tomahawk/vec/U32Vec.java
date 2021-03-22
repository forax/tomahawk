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
 * A fixed size, mutable column of 32 bits values.
 *
 * It can be created from several ways
 * <ul>
 *   <li>By wrapping an array of Java ints {@link #wrap(int[])}, or an array of Java floats {@link #wrap(float[])}
 *   <li>By mapping into memory an existing file {@link #map(U1Vec, Path)}
 *   <li>By mapping into memory a new empty file {@link #mapNew(U1Vec, Path, long)}
 *   <li>Using a builder {@link #builder(U1Vec.Builder, Path, OpenOption...)} to append values to a new mapped file
 *   <li>From an existing MemorySegment {@link #from(U1Vec, MemorySegment)}
 * </ul>
 *
 * It can load and store nulls, ints and floats
 * <ul>
 *   <li>{@link #getInt(long)} and {@link #getFloat(long)} loads a non null int / float
 *   <li>{@link #getInt(long, IntExtractor)} and {@link #getFloat(long, FloatExtractor)} loads a nullable int / float
 *   <li>{@link #setInt(long, int)} and {@link #setFloat(long, float)} stores an int / float
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * To store nulls, this Vec must be constructed with a {code validity} {@link U1Vec bit set} either at construction
 * or using {@link #withValidity(U1Vec)}.
 */
public interface U32Vec extends Vec {
  int getInt(long index);
  void setInt(long index, int value);
  float getFloat(long index);
  void setFloat(long index, float value);
  void getInt(long index, IntExtractor extractor);
  void getFloat(long index, FloatExtractor extractor);
  @Override
  U32Vec withValidity(U1Vec validity);

  interface Builder extends BaseBuilder<U32Vec> {
    U32Vec.Builder appendInt(int value) throws UncheckedIOException;
    U32Vec.Builder appendFloat(float value) throws UncheckedIOException;
    @Override
    U32Vec.Builder appendNull() throws UncheckedIOException;

    @Override
    U32Vec toVec() throws UncheckedIOException;
  }

  static U32Vec wrap(int[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }
  static U32Vec wrap(float[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  static U32Vec map(U1Vec validity, Path path) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_WRITE);
    return from(validity, memorySegment);
  }

  static U32Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length << 2);
    return map(validity, path);
  }

  static U32Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    return new VecImpl.U32Impl(data, implDataOrNull(validity));
  }

  static U32Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U32Builder(path, output, builderImpl(validityBuilder));
  }
}