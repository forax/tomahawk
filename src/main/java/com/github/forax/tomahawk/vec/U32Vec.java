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
  /**
   * Returns the value as index {@code index} as an int
   * @param index the index of the value
   * @return the value as index {@code index} as an int
   * @throws NullPointerException if the value is null
   */
  int getInt(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setInt(long index, int value);

  /**
   * Returns the value as index {@code index} as a float
   * @param index the index of the value
   * @return the value as index {@code index} as a float
   * @throws NullPointerException if the value is null
   */
  float getFloat(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setFloat(long index, float value);

  /**
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see IntBox
   */
  void getInt(long index, IntExtractor extractor);

  /**
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see FloatBox
   */
  void getFloat(long index, FloatExtractor extractor);

  @Override
  U32Vec withValidity(U1Vec validity);

  /**
   * A builder of {@link U32Vec}
   *
   * Example of usage
   * <pre>
   *   var path = Path.of("a_file_name");
   *   U32Vec vec;
   *   try(var builder = U32Vec.builder(null, path)) {
   *     builder.appendInt(3)
   *       .append(5.2f);
   *     vec = builder.toVec();
   *   }
   *   // vec is available here
   * </pre>
   * 
   * @see #builder(U1Vec.Builder, Path, OpenOption...)
   */
  interface Builder extends BaseBuilder<U32Vec> {
    /**
     * Appends an int value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    U32Vec.Builder appendInt(int value) throws UncheckedIOException;

    /**
     * Appends a float value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    U32Vec.Builder appendFloat(float value) throws UncheckedIOException;

    @Override
    U32Vec.Builder appendNull() throws UncheckedIOException;

    @Override
    U32Vec toVec() throws UncheckedIOException;
  }

  /**
   * Wraps an array of ints as a Vec
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U32Vec wrap(int[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  /**
   * Wraps an array of floats as a Vec
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U32Vec wrap(float[] array) {
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
  static U32Vec map(U1Vec validity, Path path) throws IOException {
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
  static U32Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length << 2);
    return map(validity, path);
  }

  /**
   * Creates a new Vec from an optional validity bitset (to represent null values) and a memory segment
   *
   * @param validity the validity bitset or {@code null}
   * @param data a memory segment containing the data, the byte size should be a multiple of 4
   * @return a new Vec from an optional validity bitset (to represent null values) and a memory segment
   * @throws IllegalArgumentException if the byte size of the memory segment is not a multiple of 4
   */
  static U32Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    if ((data.byteSize() & 3) != 0) {
      throw new IllegalArgumentException("the memory segment byte size should be a multiple of 4");
    }
    return new VecImpl.U32Impl(data, implDataOrNull(validity));
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
  static U32Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U32Builder(path, output, builderImpl(validityBuilder));
  }
}