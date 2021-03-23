package com.github.forax.tomahawk.vec;

import jdk.incubator.foreign.MemorySegment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.stream.IntStream;

import static com.github.forax.tomahawk.vec.VecBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.vec.VecImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.util.Objects.requireNonNull;

/**
 * A fixed size, mutable column of 16 bits values.
 *
 * It can be created
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
 * Example
 * <pre>
 *   var dataPath = dir.resolve("data");
 *   var validityPath = dir.resolve("validity");
 *
 *   U16Vec vec;
 *   try (var validityBuilder = U1Vec.builder(null, validityPath);
 *        var builder = U16Vec.builder(validityBuilder, dataPath)) {
 *     LongStream.range(0, 100_000).forEach(i -> builder.appendShort((short) i));
 *     vec = builder.toVec();
 *   }
 *   try (vec) {
 *     assertEquals(100_000, vec.length());
 *     assertEquals((short) 6, vec.getShort(6));
 *     assertEquals((short) 13_658, vec.getShort(13_658));
 *     vec.setNull(13);
 *     assertTrue(vec.isNull(13));
 *   }
 * </pre>
 *
 * To track null values, this Vec must have a {code validity} {@link U1Vec bit set}
 * either taken at construction or provided using {@link #withValidity(U1Vec)}.
 */
public interface U16Vec extends Vec {
  /**
   * Returns the value as index {@code index} as a short
   * @param index the index of the value
   * @return the value as index {@code index} as a short
   * @throws NullPointerException if the value is null
   */
  short getShort(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setShort(long index, short value);

  /**
   * Returns the value as index {@code index} as a char
   * @param index the index of the value
   * @return the value as index {@code index} as a char
   * @throws NullPointerException if the value is null
   */
  char getChar(long index);

  /**
   * Set the value at index {@index} to {@code value}
   * @param index the index of the value
   * @param value the value to set
   */
  void setChar(long index, char value);

  /**
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see ShortBox
   */
  void getShort(long index, ShortExtractor extractor);

  /**
   * Send the {@code validity} and the {@code value} at index {@code index} to the {@code extractor}
   * @param index the index of the value
   * @see CharBox
   */
  void getChar(long index, CharExtractor extractor);

  @Override
  U16Vec withValidity(U1Vec validity);

  /**
   * Returns a Stream of all the shorts
   * @return a Stream of all the shorts
   * @throws NullPointerException if one of the value is null
   *
   * @see #getShort(long)
   */
  IntStream shorts();

  /**
   * Returns a Stream of all the chars
   * @return a Stream of all the chars
   * @throws NullPointerException if one of the value is null
   *
   * @see #getShort(long)
   */
  IntStream chars();

  /**
   * A builder of {@link U16Vec}
   *
   * Example of usage
   * <pre>
   *   var path = Path.of("a_file_name");
   *   U16Vec vec;
   *   try(var builder = U16Vec.builder(null, path)) {
   *     builder.appendShort((short) 3)
   *       .append('A');
   *     vec = builder.toVec();
   *   }
   *   // vec is available here
   * </pre>
   *
   * @see #builder(U1Vec.Builder, Path, OpenOption...)
   */
  interface Builder extends BaseBuilder<U16Vec> {
    /**
     * Appends a short value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    U16Vec.Builder appendShort(short value) throws UncheckedIOException;

    /**
     * Appends a char value to the file that is mapped to a Vec
     * @param value the value to append to the file
     * @return this builder
     * @throws UncheckedIOException if an IO error occurs
     */
    U16Vec.Builder appendChar(char value) throws UncheckedIOException;

    @Override
    U16Vec.Builder appendNull() throws UncheckedIOException;

    U16Vec.Builder appendTextWrap(TextWrap textWrap) throws UncheckedIOException;
    U16Vec.Builder appendString(String s) throws UncheckedIOException;

    @Override
    U16Vec toVec();
  }

  /**
   * Wraps an array of shorts as a Vec
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U16Vec wrap(short[] array) {
    requireNonNull(array);
    var memorySegment = MemorySegment.ofArray(array);
    return from(null, memorySegment);
  }

  /**
   * Wraps an array of chars as a Vec
   * Any change to the array will be reflected to the Vec and vice versa
   *
   * @param array an array of ints
   * @return a new Vec that wraps the array
   */
  static U16Vec wrap(char[] array) {
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
  static U16Vec map(U1Vec validity, Path path) throws IOException {
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
  static U16Vec mapNew(U1Vec validity, Path path, long length) throws IOException {
    requireNonNull(path);
    VecImpl.initFile(path, length << 1);
    return map(validity, path);
  }

  /**
   * Creates a new Vec from an optional validity bitset (to represent null values) and a memory segment
   *
   * @param validity the validity bitset or {@code null}
   * @param data a memory segment containing the data, the byte size should be a multiple of 2
   * @return a new Vec from an optional validity bitset (to represent null values) and a memory segment
   * @throws IllegalArgumentException if the byte size of the memory segment is not a multiple of 2
   */
  static U16Vec from(U1Vec validity, MemorySegment data) {
    requireNonNull(data);
    if ((data.byteSize() & 1) != 0) {
      throw new IllegalArgumentException("the memory segment byte size should be a multiple of 2");
    }
    return new VecImpl.U16Impl(data, implDataOrNull(validity));
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
  static U16Vec.Builder builder(U1Vec.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
    requireNonNull(path);
    requireNonNull(openOptions);
    var output = Files.newOutputStream(path, openOptions);
    return new VecBuilderImpl.U16Builder(path, output, builderImpl(validityBuilder));
  }
}