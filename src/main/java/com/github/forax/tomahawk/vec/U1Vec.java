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
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see BooleanBox
   */
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
   * @param data a memory segment containing the data, the byte size should be a multiple of 8
   * @return a new Vec from an optional validity bitset (to represent null values) and a memory segment
   * @throws IllegalArgumentException if the byte size of the memory segment is not a multiple of 8
   */
  static U1Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    if ((data.byteSize() & 7) != 0) {
      throw new IllegalArgumentException("the memory segment byte size should be a multiple of 8");
    }
    return new VecImpl.U1Impl(data, implDataOrNull(validity));
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
  static U1Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U1Builder(path, output, builderImpl(validityBuilder));
  }
}