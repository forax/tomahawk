package com.github.forax.tomahawk;

import jdk.incubator.foreign.MemorySegment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static com.github.forax.tomahawk.DatasetBuilderImpl.builderImpl;
import static com.github.forax.tomahawk.DatasetImpl.impl;
import static com.github.forax.tomahawk.DatasetImpl.implDataOrNull;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.util.Objects.requireNonNull;

public interface Tomahawk {
  @FunctionalInterface
  interface BooleanExtractor {
    void consume(boolean validity, boolean value);
  }
  class BooleanBox implements BooleanExtractor {
    public boolean validity;
    public boolean value;

    @Override
    public void consume(boolean validity, boolean value) {
      this.validity = validity;
      this.value = value;
    }
  }
  interface ShortExtractor {
    void consume(boolean validity, short value);
  }
  class ShortBox implements ShortExtractor {
    public boolean validity;
    public short value;

    @Override
    public void consume(boolean validity, short value) {
      this.validity = validity;
      this.value = value;
    }
  }
  interface CharExtractor {
    void consume(boolean validity, char value);
  }
  class CharBox implements CharExtractor {
    public boolean validity;
    public char value;

    @Override
    public void consume(boolean validity, char value) {
      this.validity = validity;
      this.value = value;
    }
  }
  @FunctionalInterface
  interface IntExtractor {
    void consume(boolean validity, int value);
  }
  class IntBox implements IntExtractor {
    public boolean validity;
    public int value;

    @Override
    public void consume(boolean validity, int value) {
      this.validity = validity;
      this.value = value;
    }
  }
  @FunctionalInterface
  interface FloatExtractor {
    void consume(boolean validity, float value);
  }
  class FloatBox implements FloatExtractor {
    public boolean validity;
    public float value;

    @Override
    public void consume(boolean validity, float value) {
      this.validity = validity;
      this.value = value;
    }
  }
  interface ValuesExtractor {
    void consume(boolean validity, long startOffset, long endOffset);
  }
  class ValuesBox implements ValuesExtractor {
    public boolean validity;
    public long startOffset;
    public long endOffset;

    @Override
    public void consume(boolean validity, long startOffset, long endOffset) {
      this.validity = validity;
      this.startOffset = startOffset;
      this.endOffset = endOffset;
    }

    public String getString(U16Dataset dataset) {
      requireNonNull(dataset);
      if (!validity) {
        return null;
      }
      var dataSegment = DatasetImpl.impl(dataset).dataSegment();
      var start = startOffset;
      var end = endOffset;
      var length = end - start;
      var charArray = new char[(int)length];
      MemorySegment.ofArray(charArray).copyFrom(dataSegment.asSlice(start << 1L, length << 1L));
      return new String(charArray);
    }

    public int[] getIntArray(U32Dataset dataset) {
      requireNonNull(dataset);
      if (!validity) {
        return null;
      }
      var dataSegment = DatasetImpl.impl(dataset).dataSegment();
      var start = startOffset;
      var end = endOffset;
      var length = end - start;
      var intArray = new int[(int)length];
      MemorySegment.ofArray(intArray).copyFrom(dataSegment.asSlice(start << 2L, length << 2L));
      return intArray;
    }
  }

  interface UncheckedCloseable extends AutoCloseable {
    @Override
    void close() throws UncheckedIOException;
  }

  interface Dataset extends UncheckedCloseable {
    long length();
    boolean isNull(long index);
    void setNull(long index);
    Dataset withValidity(U1Dataset validity);

    interface BaseBuilder<D extends Dataset> extends UncheckedCloseable {
      long length();
      BaseBuilder<D> appendNull() throws UncheckedIOException;
      D toDataset() throws UncheckedIOException;
    }
  }

  interface U1Dataset extends Dataset {
    boolean getBoolean(long index);
    void setBoolean(long index, boolean value);
    void getBoolean(long index, BooleanExtractor extractor);

    @Override
    U1Dataset withValidity(U1Dataset validity);

    interface Builder extends BaseBuilder<U1Dataset> {
      Builder appendBoolean(boolean value);
      @Override
      Builder appendNull();

      @Override
      U1Dataset toDataset();
    }

    static U1Dataset wrap(long[] array) {
      requireNonNull(array);
      var memorySegment = MemorySegment.ofArray(array);
      return from(memorySegment, null);
    }

    static U1Dataset map(Path path, U1Dataset validity) throws IOException {
      requireNonNull(path);
      var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
      return from(memorySegment, validity);
    }

    static U1Dataset from(MemorySegment memorySegment, U1Dataset validity) {
      requireNonNull(memorySegment);
      return new DatasetImpl.U1Impl(memorySegment, implDataOrNull(validity));
    }

    static Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
      requireNonNull(path);
      requireNonNull(openOptions);
      var output = Files.newOutputStream(path, openOptions);
      return new DatasetBuilderImpl.U1Builder(path, output, builderImpl(validityBuilder));
    }
  }

  interface U8Dataset extends Dataset {
    byte getByte(long index);
    void setByte(long index, byte value);
    boolean getBoolean(long index);
    void setBoolean(long index, boolean value);
    @Override
    U8Dataset withValidity(U1Dataset validity);

    interface Builder extends BaseBuilder<U8Dataset> {
      Builder appendByte(byte value) throws UncheckedIOException;
      Builder appendBoolean(boolean value) throws UncheckedIOException;
      @Override
      Builder appendNull() throws UncheckedIOException;

      @Override
      U8Dataset toDataset();
    }

    static U8Dataset wrap(byte[] array) {
      return null;
    }
    static U8Dataset wrap(boolean[] array) {
      return null;
    }

    static U8Dataset allocate(long length) {
      return null;
    }

    static U8Dataset map(Path path) {
      return null;
    }

    static Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) {
      return null;
    }
  }

  interface U16Dataset extends Dataset {
    short getShort(long index);
    void setShort(long index, short value);
    char getChar(long index);
    void setChar(long index, char value);
    void getShort(long index, ShortExtractor extractor);
    void getChar(long index, CharExtractor extractor);
    @Override
    U16Dataset withValidity(U1Dataset validity);

    interface Builder extends BaseBuilder<U16Dataset> {
      Builder appendShort(short value) throws UncheckedIOException;
      Builder appendChar(char value) throws UncheckedIOException;
      @Override
      Builder appendNull() throws UncheckedIOException;

      Builder appendShortArray(short... array) throws UncheckedIOException;
      Builder appendCharArray(char... array) throws UncheckedIOException;
      Builder appendString(String s) throws UncheckedIOException;

      @Override
      U16Dataset toDataset();
    }

    static U16Dataset wrap(short[] array) {
      requireNonNull(array);
      var memorySegment = MemorySegment.ofArray(array);
      return from(memorySegment, null);
    }
    static U16Dataset wrap(char[] array) {
      requireNonNull(array);
      var memorySegment = MemorySegment.ofArray(array);
      return from(memorySegment, null);
    }

    static U16Dataset map(Path path, U1Dataset validity) throws IOException {
      requireNonNull(path);
      var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
      return from(memorySegment, validity);
    }

    static U16Dataset from(MemorySegment data, U1Dataset validity) {
      requireNonNull(data);
      return new DatasetImpl.U16Impl(data, implDataOrNull(validity));
    }

    static U16Dataset.Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
      requireNonNull(path);
      requireNonNull(openOptions);
      var output = Files.newOutputStream(path, openOptions);
      return new DatasetBuilderImpl.U16Builder(path, output, builderImpl(validityBuilder));
    }
  }

  interface U32Dataset extends Dataset {
    int getInt(long index);
    void setInt(long index, int value);
    float getFloat(long index);
    void setFloat(long index, float value);
    void getInt(long index, IntExtractor extractor);
    void getFloat(long index, FloatExtractor extractor);
    @Override
    U32Dataset withValidity(U1Dataset validity);

    interface Builder extends BaseBuilder<U32Dataset> {
      Builder appendInt(int value) throws UncheckedIOException;
      Builder appendFloat(float value) throws UncheckedIOException;
      @Override
      Builder appendNull() throws UncheckedIOException;

      @Override
      U32Dataset toDataset() throws UncheckedIOException;
    }

    static U32Dataset wrap(int[] array) {
      requireNonNull(array);
      var memorySegment = MemorySegment.ofArray(array);
      return from(memorySegment, null);
    }
    static U32Dataset wrap(float[] array) {
      requireNonNull(array);
      var memorySegment = MemorySegment.ofArray(array);
      return from(memorySegment, null);
    }

    static U32Dataset map(Path path, U1Dataset validity) throws IOException {
      requireNonNull(path);
      var memorySegment = MemorySegment.mapFile(path, 0, Files.size(path), READ_ONLY);
      return from(memorySegment, validity);
    }

    static U32Dataset from(MemorySegment data, U1Dataset validity) {
      requireNonNull(data);
      return new DatasetImpl.U32Impl(data, implDataOrNull(validity));
    }

    static Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) throws IOException {
      requireNonNull(path);
      requireNonNull(openOptions);
      var output = Files.newOutputStream(path, openOptions);
      return new DatasetBuilderImpl.U32Builder(path, output, builderImpl(validityBuilder));
    }
  }

  interface U64Dataset extends Dataset {
    long getLong(long index);
    void setLong(long index, long value);
    double getDouble(long index);
    void setDouble(long index, double value);

    @Override
    U64Dataset withValidity(U1Dataset validity);

    interface Builder extends BaseBuilder<U64Dataset> {
      Builder appendLong(long value) throws UncheckedIOException;
      Builder appendDouble(double value) throws UncheckedIOException;
      @Override
      Builder appendNull() throws UncheckedIOException;

      @Override
      U64Dataset toDataset();
    }

    static U64Dataset wrap(long[] array) {
      return null;
    }
    static U64Dataset wrap(double[] array) {
      return null;
    }

    static U64Dataset from(MemorySegment memorySegment) {
      return null;
    }

    static U64Dataset map(Path path) {
      return null;
    }

    static Builder builder(U1Dataset.Builder validityBuilder, Path path, OpenOption... openOptions) {
      return null;
    }
  }

  interface ListDataset<D extends Dataset> extends Dataset {
    D data();
    String getString(long index);
    void getValues(long index, ValuesExtractor extractor);
    @Override
    ListDataset<D> withValidity(U1Dataset validity);

    interface Builder<D extends Dataset, B extends BaseBuilder<D>> extends BaseBuilder<ListDataset<D>> {
      Builder<D, B> appendValues(Consumer<? super B> consumer) throws UncheckedIOException;
      @Override
      Builder<D, B> appendNull() throws UncheckedIOException;

      Builder<D, B> appendString(String s) throws UncheckedIOException;

      @Override
      ListDataset<D> toDataset();
    }

    static <D extends Dataset> ListDataset<D> from(D data, U32Dataset offset, U1Dataset validity) throws UncheckedIOException {
      if (offset.length() <= 1) {
        throw new IllegalArgumentException("offsetSegment.length is too small");
      }
      if (validity != null && (offset.length() - 1) > validity.length()) {
        throw new IllegalArgumentException("validitySegment.length is too small");
      }
      return new DatasetImpl.ListImpl<>(data, impl(data).dataSegment(), impl(offset).dataSegment(), implDataOrNull(validity));
    }

    static <D extends Dataset, B extends BaseBuilder<D>> Builder<D, B> builder(B dataBuilder, U32Dataset.Builder offsetBuilder, U1Dataset.Builder validityBuilder) {
      requireNonNull(dataBuilder);
      requireNonNull(offsetBuilder);
      return new DatasetBuilderImpl.ListBuilder<>(dataBuilder, builderImpl(offsetBuilder), builderImpl(validityBuilder));
    }
  }

  interface StructDataset extends Dataset {
    List<Dataset> fields();

    @Override
    StructDataset withValidity(U1Dataset validity);

    interface Builder extends BaseBuilder<StructDataset> {
      void attachField(BaseBuilder<?> fieldBuilder);
      Builder appendRow(Consumer<? super RowBuilder> consumer) throws UncheckedIOException;
      @Override
      Builder appendNull() throws UncheckedIOException;

      @Override
      StructDataset toDataset();
    }

    interface RowBuilder {
      RowBuilder appendNull(BaseBuilder<?> field) throws UncheckedIOException;
      RowBuilder appendShort(U16Dataset.Builder field, short value) throws UncheckedIOException;
      RowBuilder appendChar(U16Dataset.Builder field, char value) throws UncheckedIOException;
      RowBuilder appendInt(U32Dataset.Builder field, int value) throws UncheckedIOException;
      RowBuilder appendFloat(U32Dataset.Builder field, float value) throws UncheckedIOException;
      <D extends Dataset, B extends BaseBuilder<D>> RowBuilder appendValues(ListDataset.Builder<D, B> field, Consumer<? super B> consumer) throws UncheckedIOException;
      RowBuilder appendString(ListDataset.Builder<U16Dataset, U16Dataset.Builder> field, String s) throws UncheckedIOException;
    }

    static StructDataset from(U1Dataset validity, List<Dataset> fields) throws UncheckedIOException {
      return new DatasetImpl.StructImpl(implDataOrNull(validity), fields);
    }

    static Builder builder(U1Dataset.Builder validityBuilder, BaseBuilder<?>... fieldBuilders) {
       var builder = new DatasetBuilderImpl.StructBuilder(builderImpl(validityBuilder));
       for(var fieldBuilder: fieldBuilders) {
         builder.attachField(fieldBuilder);
       }
       return builder;
    }
  }
}
