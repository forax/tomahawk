package com.github.forax.tomahawk.vec;

import jdk.incubator.foreign.MemorySegment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.Stream;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.impl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.util.Objects.requireNonNull;

/**
 * A fixed size, mutable column of 1 bit values (aka a bit set).
 *
 * In term of implementation, the bits are packed into 64 bits, so {@link #length()} will always
 * return a multiple of 64.
 *
 * It can be created
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
 *   <li>{@link #getBoolean(long, BooleanBox)}  loads a nullable boolean
 *   <li>{@link #setBoolean(long, boolean)} stores a boolean
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * Example
 * <pre>
 *   var dataPath = dir.resolve("element");
 *   var validityPath = dir.resolve("validity");
 *
 *   U1Vec vec;
 *   try (var validityBuilder = U1Vec.builder(null, validityPath);
 *        var builder = U1Vec.builder(validityBuilder, dataPath)) {
 *     LongStream.range(0, 100_000).forEach(i -> builder.appendBoolean(i % 2 == 0));
 *     vec = builder.toVec();
 *   }
 *   try (vec) {
 *     assertEquals(100_032, vec.length());   // values are aligned to 64 bits
 *     assertTrue(vec.getBoolean(6));
 *     assertFalse(vec.getBoolean(777));
 *     vec.setNull(13);
 *     assertTrue(vec.isNull(13));
 *   }
 * </pre>
 *
 * To track null values, this Vec must have a {code validity} {@link U1Vec bit set}
 * either taken at construction or provided using {@link #withValidity(U1Vec)}.
 */
public interface U1Vec extends Vec {
  /**
   * Returns the value as index {@code index} as a boolean
   * @param index the index of the value
   * @return the value as index {@code index} as a boolean
   * @throws NullPointerException if the value is null
   */
  boolean getBoolean(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setBoolean(long index, boolean value);

  /**
   * Fill the box with {@code validity} and the {@code value} at index {@code index}
   * @param index the index of the value
   * @param box the box that will be filled
   * @return the box taken as parameter filled with the {@code validity} and the {@code value}
   */
  BooleanBox getBoolean(long index, BooleanBox box);

  @Override
  U1Vec withValidity(U1Vec validity);

  /**
   * Returns a Stream of all the booleans
   * @return a Stream of all the booleans
   * @throws NullPointerException if one of the value is null
   *
   * @see #getBoolean(long)
   */
  Stream<Boolean> allBooleans();

  /**
   * A builder of {@link U1Vec}
   *
   * Example of usage
   * <pre>
   *   var path = Path.of("a_file_name");
   *   U1Vec vec;
   *   try(var builder = U1Vec.builder(null, path)) {
   *     builder.appendBoolean(true)
   *       .appendBoolean(false);
   *     vec = builder.toVec();
   *   }
   *   // vec is available here
   * </pre>
   *
   * @see #builder(U1Vec.Builder, Path, OpenOption...)
   */
  interface Builder extends BaseBuilder<U1Vec> {
    /**
     * Appends a boolean value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    U1Vec.Builder appendBoolean(boolean value);

    @Override
    U1Vec.Builder appendNull();

    @Override
    U1Vec toVec();
  }

  /**
   * Wraps an array of longs as a bit set
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U1Vec wrap(long[] array) {
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
  static U1Vec map(U1Vec validity, Path path) throws IOException {
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
  static U1Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length >> 3);
    return map(validity, path);
  }

  /**
   * Creates a new Vec from an optional validity bitset (to represent null values) and a memory segment
   *
   * @param validity the validity bitset or {@code null}
   * @param data a memory segment containing the element, the byte size should be a multiple of 8
   * @return a new Vec from an optional validity bitset (to represent null values) and a memory segment
   * @throws IllegalArgumentException if the byte size of the memory segment is not a multiple of 8
   */
  static U1Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    if ((data.byteSize() & 7) != 0) {
      throw new IllegalArgumentException("the memory segment byte size should be a multiple of 8");
    }
    if (validity != null  && impl(validity).validitySegment() != null) {
      throw new IllegalArgumentException("validity can not have a validity vec");
    }
    VecImpl.register(data);
    return new VecImpl.U1Impl(data, implDataOrNull(validity));
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
  static U1Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U1Builder(path, output, builderImpl(validityBuilder));
  }
}