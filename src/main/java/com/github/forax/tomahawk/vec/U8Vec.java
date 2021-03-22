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
 * A fixed size, mutable column of 8 bits values.
 *
 * It can be created from several ways
 * <ul>
 *   <li>By wrapping an array of Java ints {@link #wrap(byte[])}
 *   <li>By mapping into memory an existing file {@link #map(U1Vec, Path)}
 *   <li>By mapping into memory a new empty file {@link #mapNew(U1Vec, Path, long)}
 *   <li>Using a builder {@link #builder(U1Vec.Builder, Path, OpenOption...)} to append values to a new mapped file
 *   <li>From an existing MemorySegment {@link #from(U1Vec, MemorySegment)}
 * </ul>
 *
 * It can load and store nulls and bytes
 * <ul>
 *   <li>{@link #getByte(long)} loads a non null byte
 *   <li>{@link #getByte(long, ByteExtractor)} loads a nullable byte
 *   <li>{@link #setByte(long, byte)} stores a byte
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * To store nulls, this Vec must be constructed with a {code validity} {@link U1Vec bit set} either at construction
 * or using {@link #withValidity(U1Vec)}.
 */
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
    return from(null, memorySegment);
  }

  static U8Vec map(U1Vec validity, Path path) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_WRITE);
    return from(validity, memorySegment);
  }

  static U8Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length);
    return map(validity, path);
  }

  static U8Vec from(U1Vec validity, MemorySegment data) {
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