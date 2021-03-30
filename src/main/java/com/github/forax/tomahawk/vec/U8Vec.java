package com.github.forax.tomahawk.vec;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.impl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.IntStream;

import jdk.incubator.foreign.MemorySegment;

/**
 * A fixed size, mutable column of 8 bits values.
 *
 * It can be created
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
 *   <li>{@link #getByte(long, ByteBox)} loads a nullable byte
 *   <li>{@link #setByte(long, byte)} stores a byte
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * Example
 * <pre>
 *   var dataPath = dir.resolve("element");
 *   var validityPath = dir.resolve("validity");
 *
 *   U8Vec vec;
 *   try (var validityBuilder = U1Vec.builder(null, validityPath);
 *        var builder = U8Vec.builder(validityBuilder, dataPath)) {
 *     LongStream.range(0, 100_000).forEach(i -> builder.appendByte((byte) i));
 *     vec = builder.toVec();
 *   }
 *   try (vec) {
 *     assertEquals(100_000, vec.length());
 *     assertEquals((byte) 6, vec.getByte(6));
 *     assertEquals((byte) 120, vec.getByte(120));
 *     vec.setNull(13);
 *     assertTrue(vec.isNull(13));
 *   }
 * </pre>
 *
 * To track null values, this Vec must have a {code validity} {@link U1Vec bit set}
 * either taken at construction or provided using {@link #withValidity(U1Vec)}.
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
   * Fill the box with {@code validity} and the {@code value} at index {@code index}
   * @param index the index of the value
   * @param box the box that will be filled
   * @return the box taken as parameter filled with the {@code validity} and the {@code value}
   */
  ByteBox getByte(long index, ByteBox box);

  @Override
  U8Vec withValidity(U1Vec validity);

  /**
   * Returns a Stream of all the bytes
   * @return a Stream of all the bytes
   * @throws NullPointerException if one of the value is null
   *
   * @see #getByte(long)
   */
  IntStream allBytes();

  /**
   * A builder of {@link U8Vec}
   *
   * Example of usage
   * <pre>
   *   var path = Path.of("a_file_name");
   *   U8Vec vec;
   *   try(var builder = U8Vec.builder(null, path)) {
   *     builder.appendInt((byte) 3)
   *       .append((byte) -5);
   *     vec = builder.toVec();
   *   }
   *   // vec is available here
   * </pre>
   *
   * @see #builder(U1Vec.Builder, Path, OpenOption...)
   */
  interface Builder extends BaseBuilder<U8Vec> {
    /**
     * Appends a byte value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
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
   * @param data a memory segment containing the element
   * @return a new Vec from an optional validity bitset (to represent null values) and a memory segment
   */
  static U8Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    if (validity != null  && impl(validity).validitySegment() != null) {
      throw new IllegalArgumentException("validity can not have a validity vec");
    }
    VecImpl.register(data);
    return new VecImpl.U8Impl(data, implDataOrNull(validity));
  }

  /**
   * Create a Vec builder that will append values to a file before creating a Vec on the values appended
   *
   * @param validityBuilder a builder able to create the validity bit set or {@code null}
   * @param path a path to the file that will be created
   * @param openOptions the option used to create the file
   * @return a Vec builder that will append the values to a file before creating a Vec on that file
   * @throws IOException if an IO error occurs
   */
  static U8Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U8Builder(path, output, builderImpl(validityBuilder));
  }
}