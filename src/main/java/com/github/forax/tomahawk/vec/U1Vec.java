package com.github.forax.tomahawk.vec;

import jdk.incubator.foreign.MemorySegment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.util.Objects.requireNonNull;

/**
 * A fixed size, mutable column of 1 bit values (aka a bit set).
 *
 * In term of implementation, the bits are packed into 64 bits, so {@link #length()} will always
 * return a multiple of 64.
 *
 * It can be created from several ways
 * <ul>
 *   <li>By mapping into memory an existing file {@link #map(U1Vec, Path)}
 *   <li>By mapping into memory a new empty file {@link #mapNew(U1Vec, Path, long)}
 *   <li>Using a builder {@link #builder(U1Vec.Builder, Path, OpenOption...)} to append values to a new mapped file
 *   <li>From an existing MemorySegment {@link #from(U1Vec, MemorySegment)}
 * </ul>
 *
 * It can load and store nulls and booleans
 * <ul>
 *   <li>{@link #getBoolean(long)} loads a non null boolean
 *   <li>{@link #getBoolean(long, BooleanExtractor)}  loads a nullable boolean
 *   <li>{@link #setBoolean(long, boolean)} stores a boolean
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * To store nulls, this Vec must be constructed with a {code validity} {@link U1Vec bit set} either at construction
 * or using {@link #withValidity(U1Vec)}.
 */
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
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_WRITE);
    return from(validity, memorySegment);
  }

  static U1Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length >> 3);
    return map(validity, path);
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