package com.github.forax.tomahawk.vec;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import jdk.incubator.foreign.MemorySegment;

/**
 * A fixed size, mutable column of 64 bits values.
 *
 * It can be created
 * <ul>
 *   <li>By wrapping an array of Java floats {@link #wrap(long[])}, or an array of Java doubles {@link #wrap(double[])}
 *   <li>By mapping into memory an existing file {@link #map(U1Vec, Path)}
 *   <li>By mapping into memory a new empty file {@link #mapNew(U1Vec, Path, long)}
 *   <li>Using a builder {@link #builder(U1Vec.Builder, Path, OpenOption...)} to append values to a new mapped file
 *   <li>From an existing MemorySegment {@link #from(U1Vec, MemorySegment)}
 * </ul>
 *
 * It can load and store nulls, longs and doubles
 * <ul>
 *   <li>{@link #getLong(long)} and {@link #getDouble(long)} loads a non null long / double
 *   <li>{@link #getLong(long, LongExtractor)} and {@link #getDouble(long, DoubleExtractor)}
 *       loads a nullable long / double
 *   <li>{@link #setLong(long, long)} and {@link #setDouble(long, double)} stores a long / double
 *   <li>{@link #isNull(long)} checks if a value is {code null}
 *   <li>{@link #setNull(long)} stores {code null}
 * </ul>
 *
 * Example
 * <pre>
 *   var dataPath = dir.resolve("data");
 *   var validityPath = dir.resolve("validity");
 *
 *   U64Vec vec;
 *   try (var validityBuilder = U1Vec.builder(null, validityPath);
 *        var builder = U64Vec.builder(validityBuilder, dataPath)) {
 *     LongStream.range(0, 100_000).forEach(i -> builder.appendLong(i));
 *     vec = builder.toVec();
 *   }
 *   try (vec) {
 *     assertEquals(100_000, vec.length());
 *     assertEquals(6, vec.getLong(6));
 *     assertEquals(78_453, vec.getLong(78_453));
 *     vec.setNull(13);
 *     assertTrue(vec.isNull(13));
 *   }
 * </pre>
 *
 * To track null values, this Vec must have a {code validity} {@link U1Vec bit set}
 * either taken at construction or provided using {@link #withValidity(U1Vec)}.
 */
public interface U64Vec extends Vec {
  /**
   * Returns the value as index {@code index} as a double
   * @param index the index of the value
   * @return the value as index {@code index} as a double
   * @throws NullPointerException if the value is null
   */
  long getLong(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setLong(long index, long value);

  /**
   * Returns the value as index {@code index} as a double
   * @param index the index of the value
   * @return the value as index {@code index} as a double
   * @throws NullPointerException if the value is null
   */
  double getDouble(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setDouble(long index, double value);

  /**
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see LongBox
   */
  void getLong(long index, LongExtractor extractor);

  /**
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see DoubleBox
   */
  void getDouble(long index, DoubleExtractor extractor);

  @Override
  U64Vec withValidity(U1Vec validity);

  /**
   * Returns a Stream of all the longs
   * @return a Stream of all the longs
   * @throws NullPointerException if one of the value is null
   *
   * @see #getLong(long)
   */
  LongStream longs();

  /**
   * Returns a Stream of all the doubles
   * @return a Stream of all the doubles
   * @throws NullPointerException if one of the value is null
   *
   * @see #getDouble(long)
   */
  DoubleStream doubles();

  /**
   * A builder of {@link U64Vec}
   *
   * Example of usage
   * <pre>
   *   var path = Path.of("a_file_name");
   *   U64Vec vec;
   *   try(var builder = U64Vec.builder(null, path)) {
   *     builder.appendInt(3L)
   *       .append(5.2);
   *     vec = builder.toVec();
   *   }
   *   // vec is available here
   * </pre>
   *
   * @see #builder(U1Vec.Builder, Path, OpenOption...)
   */
  interface Builder extends BaseBuilder<U64Vec> {
    /**
     * Appends a long value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    U64Vec.Builder appendLong(long value) throws UncheckedIOException;

    /**
     * Appends a double value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    U64Vec.Builder appendDouble(double value) throws UncheckedIOException;

    @Override
    U64Vec.Builder appendNull() throws UncheckedIOException;

    @Override
    U64Vec toVec();
  }

  /**
   * Wraps an array of longs as a Vec
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U64Vec wrap(long[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  /**
   * Wraps an array of doubles as a Vec
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U64Vec wrap(double[] array) {
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
  static U64Vec map(U1Vec validity, Path path) throws IOException {
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
  static U64Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length << 3);
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
  static U64Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    if ((data.byteSize() & 7) != 0) {
      throw new IllegalArgumentException("the memory segment byte size should be a multiple of 8");
    }
    return new VecImpl.U64Impl(data, implDataOrNull(validity));
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
  static U64Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U64Builder(path, output, builderImpl(validityBuilder));
  }
}