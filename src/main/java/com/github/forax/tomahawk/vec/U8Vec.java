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
  /**
   * Returns the value as index {@code index} as a byte
   * @param index the index of the value
   * @return the value as index {@code index} as a byte
   * @throws NullPointerException if the value is null
   */
  byte getByte(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setByte(long index, byte value);

  /**
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see ByteBox
   */
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

  /**
   * Wraps an array of bytes as a Vec
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U8Vec wrap(byte[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  /**
   * Map an existing file in memory as a Vec
   *
   * @param validity the validity bitset or {@code null}
   * @param path the path of the file to map
   * @return a new Vec using the file content as memory
   * @throws IOException if an IO error occurs
   */
  static U8Vec map(U1Vec validity, Path path) throws IOException {
    requireNonNull(path);
    var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_WRITE);
    return from(validity, memorySegment);
  }

  /**
   * Creates a new file able to store {@code length} values and memory map it to a new Vec
   *
   * @param validity the validity bitset or {@code null}
   * @param path the path of the file to create
   * @param length the maximum number of values
   * @return a new file able to store {@code length} values and memory map it to a new Vec
   * @throws IOException if an IO error occurs
   */
  static U8Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length);
    return map(validity, path);
  }

  /**
   * Creates a new Vec from an optional validity bitset (to represent null values) and a memory segment
   *
   * @param validity the validity bitset or {@code null}
   * @param data a memory segment containing the data
   * @return a new Vec from an optional validity bitset (to represent null values) and a memory segment
   */
  static U8Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    return new VecImpl.U8Impl(data, implDataOrNull(validity));
  }

  /**
   * Create a Vec builder that will append values to a file before creating a Vec on the values appended
   *
   * @param validityBuilder a builder able to create the validity bit set or {@code null}
   * @param path a path to the file that will be created
   * @param openOptions the option used to create the file
   * @return a Vec builder that will append values to a file before creating a Vec on the values appended
   * @throws IOException if an IO error occurs
   */
  static U8Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U8Builder(path, output, builderImpl(validityBuilder));
  }
}