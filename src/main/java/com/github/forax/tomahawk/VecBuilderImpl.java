package com.github.forax.tomahawk;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.function.Consumer;

import com.github.forax.tomahawk.Vec.BaseBuilder;
import com.github.forax.tomahawk.StructVec.RowBuilder;

interface VecBuilderImpl {
  private static IllegalStateException doNotSupportNull() {
    throw new IllegalStateException("this builder does not support null");
  }

  private static IllegalStateException fieldValueAlreadyAppended() {
    throw new IllegalStateException("field already has a value");
  }

  static BaseImpl builderImpl(Vec.BaseBuilder<?> builder) {
    return (BaseImpl) builder;
  }
  static U1Builder builderImpl(U1Vec.Builder builder) {
    return (U1Builder) builder;
  }
  static U16Builder builderImpl(U16Vec.Builder builder) {
    return (U16Builder) builder;
  }
  static U32Builder builderImpl(U32Vec.Builder builder) {
    return (U32Builder) builder;
  }
  static <D extends Vec, B extends BaseBuilder<D>> ListBuilder<D, B> builderImpl(ListVec.Builder<D, B> builder) {
    return (ListBuilder<D, B>) builder;
  }

  abstract class BaseImpl {
    private int ordinal = -1;

    int ordinal() {
      if (ordinal == -1) {
        throw new IllegalStateException("field not associated to this struct");
      }
      return ordinal;
    }

    void ordinal(int ordinal) {
      if (this.ordinal != -1) {
        throw new IllegalStateException("already used as a field by another struct");
      }
      this.ordinal = ordinal;
    }
  }

  final class U1Builder extends BaseImpl implements U1Vec.Builder {
    private final Path path;
    private final OutputStream output;
    private final U1Builder validityBuilder;
    private final ByteBuffer buffer;
    private long length;
    private long current;
    private int position;

    U1Builder(Path path, OutputStream output, U1Builder validityBuilder, ByteBuffer buffer) {
      this.path = path;
      this.output = output;
      this.validityBuilder = validityBuilder;
      this.buffer = buffer;
    }

    U1Builder(Path path, OutputStream output, U1Builder validityBuilder) {
      this(path, output, validityBuilder, ByteBuffer.allocate(8192).order(LITTLE_ENDIAN));
    }

    private void flush() throws UncheckedIOException {
      try {
        output.write(buffer.array(), 0, buffer.position());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      buffer.clear();
    }

    @Override
    public void close() throws UncheckedIOException {
      if (position != 0) {
        if (!buffer.hasRemaining()) {
          flush();
        }
        buffer.putLong(current);
        position = 0;
      }
      flush();
      try {
        output.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (validityBuilder != null) {
        validityBuilder.close();
      }
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public U1Vec.Builder appendBoolean(boolean value) {
      if (value) {
        current |= 1L << position;
      }
      if (++position == 64) {
        if (!buffer.hasRemaining()) {
          flush();
        }
        buffer.putLong(current);
        position = 0;
      }
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U1Vec.Builder appendNull() {
      if (validityBuilder == null) {
        throw doNotSupportNull();
      }
      if (++position == 64) {
        if (!buffer.hasRemaining()) {
          flush();
        }
        buffer.putLong(current);
        position = 0;
      }
      validityBuilder.appendBoolean(false);
      length++;
      return this;
    }

    @Override
    public U1Vec toVec() {
      close();
      try {
        return U1Vec.map(path, validityBuilder == null ? null: validityBuilder.toVec());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  final class U8Builder extends BaseImpl implements U8Vec.Builder {
    private final Path path;
    private final OutputStream output;
    private final U1Builder validityBuilder;
    private final ByteBuffer buffer;
    private long length;

    U8Builder(Path path, OutputStream output, U1Builder validityBuilder, ByteBuffer buffer) {
      this.path = path;
      this.output = output;
      this.validityBuilder = validityBuilder;
      this.buffer = buffer;
    }

    U8Builder(Path path, OutputStream output, U1Builder validityBuilder) {
      this(path, output, validityBuilder, ByteBuffer.allocate(8192).order(LITTLE_ENDIAN));
    }

    private void flush() {
      try {
        output.write(buffer.array(), 0, buffer.position());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      buffer.clear();
    }

    @Override
    public void close() throws UncheckedIOException {
      flush();
      try {
        output.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (validityBuilder != null) {
        validityBuilder.close();
      }
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public U8Vec.Builder appendByte(byte value) {
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.put(value);
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U8Vec.Builder appendNull() {
      if (validityBuilder == null) {
        throw doNotSupportNull();
      }
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.put((byte) 0);
      validityBuilder.appendBoolean(false);
      length++;
      return this;
    }

    @Override
    public U8Vec toVec() {
      close();
      try {
        return U8Vec.map(path, validityBuilder == null? null: validityBuilder.toVec());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  final class U16Builder extends BaseImpl implements U16Vec.Builder {
    private final Path path;
    private final OutputStream output;
    private final U1Builder validityBuilder;
    private final ByteBuffer buffer;
    private long length;

    U16Builder(Path path, OutputStream output, U1Builder validityBuilder, ByteBuffer buffer) {
      this.path = path;
      this.output = output;
      this.validityBuilder = validityBuilder;
      this.buffer = buffer;
    }

    U16Builder(Path path, OutputStream output, U1Builder validityBuilder) {
      this(path, output, validityBuilder, ByteBuffer.allocate(8192).order(LITTLE_ENDIAN));
    }

    private void flush() {
      try {
        output.write(buffer.array(), 0, buffer.position());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      buffer.clear();
    }

    @Override
    public void close() throws UncheckedIOException {
      flush();
      try {
        output.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (validityBuilder != null) {
        validityBuilder.close();
      }
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public U16Vec.Builder appendShort(short value) {
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putShort(value);
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U16Vec.Builder appendChar(char value) {
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putChar(value);
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U16Vec.Builder appendNull() {
      if (validityBuilder == null) {
        throw doNotSupportNull();
      }
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putShort((short) 0);
      validityBuilder.appendBoolean(false);
      length++;
      return this;
    }

    @Override
    public U16Vec.Builder appendString(String s) throws UncheckedIOException {
      for(var i = 0; i < s.length(); i++) {
        appendChar(s.charAt(i));
      }
      return this;
    }

    @Override
    public U16Vec.Builder appendCharArray(char... array) throws UncheckedIOException {
      for (char value : array) {
        appendChar(value);
      }
      return this;
    }

    @Override
    public U16Vec.Builder appendShortArray(short... array) throws UncheckedIOException {
      for (short value : array) {
        appendShort(value);
      }
      return this;
    }

    @Override
    public U16Vec toVec() {
      close();
      try {
        return U16Vec.map(path, validityBuilder == null? null: validityBuilder.toVec());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  final class U32Builder extends BaseImpl implements U32Vec.Builder {
    private final Path path;
    private final OutputStream output;
    private final U1Builder validityBuilder;
    private final ByteBuffer buffer;
    private long length;

    U32Builder(Path path, OutputStream output, U1Builder validityBuilder, ByteBuffer buffer) {
      this.path = path;
      this.output = output;
      this.validityBuilder = validityBuilder;
      this.buffer = buffer;
    }

    U32Builder(Path path, OutputStream output, U1Builder validityBuilder) {
      this(path, output, validityBuilder, ByteBuffer.allocate(8192).order(LITTLE_ENDIAN));
    }

    private void flush() {
      try {
        output.write(buffer.array(), 0, buffer.position());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      buffer.clear();
    }

    @Override
    public void close() throws UncheckedIOException {
      flush();
      try {
        output.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (validityBuilder != null) {
        validityBuilder.close();
      }
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public U32Vec.Builder appendInt(int value) {
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putInt(value);
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U32Vec.Builder appendFloat(float value) {
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putFloat(value);
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U32Vec.Builder appendNull() {
      if (validityBuilder == null) {
        throw doNotSupportNull();
      }
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putInt(0);
      validityBuilder.appendBoolean(false);
      length++;
      return this;
    }

    @Override
    public U32Vec toVec() {
      close();
      try {
        return U32Vec.map(path, validityBuilder == null? null: validityBuilder.toVec());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  final class U64Builder extends BaseImpl implements U64Vec.Builder {
    private final Path path;
    private final OutputStream output;
    private final U1Builder validityBuilder;
    private final ByteBuffer buffer;
    private long length;

    U64Builder(Path path, OutputStream output, U1Builder validityBuilder, ByteBuffer buffer) {
      this.path = path;
      this.output = output;
      this.validityBuilder = validityBuilder;
      this.buffer = buffer;
    }

    U64Builder(Path path, OutputStream output, U1Builder validityBuilder) {
      this(path, output, validityBuilder, ByteBuffer.allocate(8192).order(LITTLE_ENDIAN));
    }

    private void flush() {
      try {
        output.write(buffer.array(), 0, buffer.position());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      buffer.clear();
    }

    @Override
    public void close() throws UncheckedIOException {
      flush();
      try {
        output.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      if (validityBuilder != null) {
        validityBuilder.close();
      }
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public U64Vec.Builder appendLong(long value) {
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putLong(value);
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U64Vec.Builder appendDouble(double value) {
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putDouble(value);
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    @Override
    public U64Vec.Builder appendNull() {
      if (validityBuilder == null) {
        throw doNotSupportNull();
      }
      if (!buffer.hasRemaining()) {
        flush();
      }
      buffer.putLong(0);
      validityBuilder.appendBoolean(false);
      length++;
      return this;
    }

    @Override
    public U64Vec toVec() {
      close();
      try {
        return U64Vec.map(path, validityBuilder == null? null: validityBuilder.toVec());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  final class ListBuilder<D extends Vec, B extends BaseBuilder<D>> extends BaseImpl implements ListVec.Builder<D, B> {
    private final B dataBuilder;
    private final U32Builder offsetBuilder;
    private final U1Builder validityBuilder;
    private int offset;
    private boolean closed;

    ListBuilder(B dataBuilder, U32Builder offsetBuilder, U1Builder validityBuilder) {
      this.dataBuilder = dataBuilder;
      this.offsetBuilder = offsetBuilder;
      this.validityBuilder = validityBuilder;
    }

    @Override
    public void close() throws UncheckedIOException {
      if (closed) {  // implements idempotence
        return;
      }
      closed = true;
      offsetBuilder.appendInt(offset);  // last offsetSegment
      dataBuilder.close();
      offsetBuilder.close();
      if (validityBuilder != null) {
        validityBuilder.close();
      }
    }

    @Override
    public long length() {
      return offsetBuilder.length - (closed? 1: 0);
    }

    @Override
    public ListVec.Builder<D, B> appendValues(Consumer<? super B> consumer) {
      requireNonNull(consumer);
      consumer.accept(dataBuilder);
      offsetBuilder.appendInt(offset);
      var length = dataBuilder.length();
      if (length > Integer.MAX_VALUE) {
        throw new ArithmeticException("overflow the size of an int");
      }
      offset = (int) length;
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      return this;
    }

    @Override
    public ListVec.Builder<D, B> appendNull() {
      if (validityBuilder == null) {
        throw doNotSupportNull();
      }
      offsetBuilder.appendInt(offset);
      validityBuilder.appendBoolean(false);
      return this;
    }

    @Override
    public ListVec.Builder<D, B> appendString(String s) {
      if (s == null) {
        return appendNull();
      }
      appendValues(b -> {
        if (!(b instanceof U16Builder stringBuilder)) {
          throw new IllegalStateException("getString is only supported on U16Dataset");
        }
        stringBuilder.appendString(s);
      });
      return this;
    }

    @Override
    public ListVec<D> toVec() {
      close();
      return ListVec.from(dataBuilder.toVec(), offsetBuilder.toVec(), validityBuilder == null? null: validityBuilder.toVec());
    }
  }

  final class StructBuilder extends BaseImpl implements StructVec.Builder {
    private final U1Builder validityBuilder;
    private final ArrayList<BaseBuilder<?>> fieldBuilders = new ArrayList<>();
    private long length;
    private final RowBuilderImpl rowBuilder = new RowBuilderImpl();

    StructBuilder(U1Builder validityBuilder) {
      this.validityBuilder = validityBuilder;
    }

    @Override
    public void attachField(BaseBuilder<?> fieldBuilder) {
      if (fieldBuilder.length() > length) {
        throw new IllegalStateException("fieldBuilder.length > length");
      }
      var impl = builderImpl(fieldBuilder);
      impl.ordinal(fieldBuilders.size());  // handshake
      fieldBuilders.add(fieldBuilder);
      // add enough null to align with the other fields
      for (var i = fieldBuilder.length(); i < length; i++) {
        fieldBuilder.appendNull();
      }
    }

    @Override
    public void close() throws UncheckedIOException {
      for(var fieldBuilder: fieldBuilders) {
        fieldBuilder.close();
      }
      if (validityBuilder != null) {
        validityBuilder.close();
      }
    }

    @Override
    public long length() {
      return length;
    }

    @Override
    public StructVec.Builder appendNull() throws UncheckedIOException {
      if (validityBuilder == null) {
        throw doNotSupportNull();
      }
      for(var fieldBuilder: fieldBuilders) {
        fieldBuilder.appendNull();
      }
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(false);
      }
      length++;
      return this;
    }

    @Override
    public StructVec.Builder appendRow(Consumer<? super RowBuilder> consumer) throws UncheckedIOException {
      requireNonNull(consumer);
      consumer.accept(rowBuilder);
      rowBuilder.end();
      if (validityBuilder != null) {
        validityBuilder.appendBoolean(true);
      }
      length++;
      return this;
    }

    private class RowBuilderImpl implements RowBuilder {
      private final BitSet bits = new BitSet();

      @Override
      public RowBuilder appendNull(BaseBuilder<?> field) {
        requireNonNull(field);
        var impl =  builderImpl(field);
        var ordinal = impl.ordinal();
        if (bits.get(ordinal)) {
          throw fieldValueAlreadyAppended();
        }
        field.appendNull();
        bits.set(ordinal);
        return this;
      }

      @Override
      public RowBuilder appendShort(U16Vec.Builder field, short value) {
        requireNonNull(field);
        var impl =  builderImpl(field);
        var ordinal = impl.ordinal();
        if (bits.get(ordinal)) {
          throw fieldValueAlreadyAppended();
        }
        field.appendShort(value);
        bits.set(ordinal);
        return this;
      }

      @Override
      public RowBuilder appendChar(U16Vec.Builder field, char value) {
        requireNonNull(field);
        var impl =  builderImpl(field);
        var ordinal = impl.ordinal();
        if (bits.get(ordinal)) {
          throw fieldValueAlreadyAppended();
        }
        field.appendChar(value);
        bits.set(ordinal);
        return this;
      }

      @Override
      public RowBuilder appendInt(U32Vec.Builder field, int value) {
        requireNonNull(field);
        var impl =  builderImpl(field);
        var ordinal = impl.ordinal();
        if (bits.get(ordinal)) {
          throw fieldValueAlreadyAppended();
        }
        field.appendInt(value);
        bits.set(ordinal);
        return this;
      }

      @Override
      public RowBuilder appendFloat(U32Vec.Builder field, float value) {
        requireNonNull(field);
        var impl =  builderImpl(field);
        var ordinal = impl.ordinal();
        if (bits.get(ordinal)) {
          throw fieldValueAlreadyAppended();
        }
        field.appendFloat(value);
        bits.set(ordinal);
        return this;
      }

      @Override
      public <D extends Vec, B extends BaseBuilder<D>> RowBuilder appendValues(ListVec.Builder<D, B> field, Consumer<? super B> consumer) {
        requireNonNull(field);
        var impl =  builderImpl(field);
        var ordinal = impl.ordinal();
        if (bits.get(ordinal)) {
          throw fieldValueAlreadyAppended();
        }
        field.appendValues(consumer);
        bits.set(ordinal);
        return this;
      }

      @Override
      public RowBuilder appendString(ListVec.Builder<U16Vec, U16Vec.Builder> field, String s) {
        return appendValues(field, b -> b.appendString(s));
      }

      void end() {
        // add nulls to all column values that were not appended
        for(var i = bits.nextClearBit(0); i != -1 && i < fieldBuilders.size(); i = bits.nextClearBit(i + 1)) {
          fieldBuilders.get(i).appendNull();
        }
        bits.clear();
      }
    }

    @Override
    public StructVec toVec() {
      close();
      var fields = fieldBuilders.stream().<Vec>map(BaseBuilder::toVec).toList();
      return StructVec.from(validityBuilder == null? null: validityBuilder.toVec(), fields);
    }
  }
}
