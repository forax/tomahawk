package com.github.forax.tomahawk.vec;

import jdk.incubator.foreign.MemorySegment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.requireNonNull;
import static jdk.incubator.foreign.MemoryLayout.PathElement.sequenceElement;
import static jdk.incubator.foreign.MemoryLayout.ofSequence;
import static jdk.incubator.foreign.MemoryLayout.ofValueBits;

/**
 * Vec implementation.
 */
interface VecImpl {
  private static IllegalStateException doNotSupportNull() {
    throw new IllegalStateException("this dataset do not support null");
  }

  static NullPointerException valueIsNull() {
    throw new NullPointerException("the value is null");
  }

  private static IllegalArgumentException invalidLength(Vec self, Vec validity) {
    throw new IllegalArgumentException("invalid length: length " + self.length() + " != validitySegment length " + validity.length());
  }

  class CloseHelper {
    private static final Class<?> NON_CLOSEABLE_SHARED_SCOPE;
    static {
      try {
        NON_CLOSEABLE_SHARED_SCOPE = Class.forName("jdk.internal.foreign.MemoryScope$NonCloseableSharedScope");
      } catch (ClassNotFoundException e) {
        throw (NoClassDefFoundError) new NoClassDefFoundError().initCause(e);
      }
    }

    private static void close(MemorySegment memorySegment) {
      var scope = memorySegment.scope();
      if (CloseHelper.NON_CLOSEABLE_SHARED_SCOPE.isInstance(scope)) {
        return;  // scope non closeable
      }
      if (scope.isAlive()) {
        scope.close();
      }
    }
  }

  MemorySegment dataSegment();

  static VecImpl impl(Vec vec) {
    return (VecImpl) vec;
  }
  static U1Impl impl(U1Vec dataset) {
    return (U1Impl) dataset;
  }
  static U16Impl impl(U16Vec dataset) {
    return (U16Impl) dataset;
  }
  static U32Impl impl(U32Vec dataset) {
    return (U32Impl) dataset;
  }

  static MemorySegment implDataOrNull(U1Vec validity) {
    if (validity == null) {
      return null;
    }
    return impl(validity).dataSegment;
  }

  static void initFile(Path path, long length) throws IOException {
    requireNonNull(path, "path");
    if (length < 0) {
      throw new IllegalArgumentException("length < 0");
    }
    if (length == 0) {  // empty file
      return;
    }
    try(var channel = FileChannel.open(path, StandardOpenOption.READ,  StandardOpenOption.WRITE)) {
      channel.write(ByteBuffer.wrap(new byte[1]), length - 1);
    }
  }

  record U1Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements VecImpl, U1Vec {
    static final VarHandle HANDLE = ofSequence(ofValueBits(64, LITTLE_ENDIAN))
        .varHandle(long.class, sequenceElement());

    private static void setRawBoolean(MemorySegment array, long index, boolean value) {
      var longIndex = index >>> 6;
      var bits = (long) HANDLE.get(array, longIndex);
      if (value) {
        bits |= 1L << index;
      } else {
        bits &= ~(1L << index);
      }
      HANDLE.set(array, longIndex, bits);
    }
    private static boolean getRawBoolean(MemorySegment array, long index) {
      var longIndex = index >>> 6;
      var bits = (long) HANDLE.get(array, longIndex);
      return (bits & (1L << index)) != 0;
    }

    @Override
    public void close() {
      try {
        CloseHelper.close(dataSegment);
      } finally {
        if (validitySegment != null) {
          CloseHelper.close(validitySegment);
        }
      }
    }

    @Override
    public long length() {
      return dataSegment.byteSize() << 3;
    }

    @Override
    public boolean isNull(long index) {
      if (validitySegment == null) {
        return false;
      }
      return !U1Impl.getRawBoolean(validitySegment, index);
    }

    @Override
    public void setNull(long index) {
      if (validitySegment == null) {
        throw doNotSupportNull();
      }
      setRawBoolean(dataSegment, index, false);
      U1Impl.setRawBoolean(validitySegment, index, false);
    }

    @Override
    public boolean getBoolean(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return getRawBoolean(dataSegment, index);
    }

    @Override
    public void setBoolean(long index, boolean value) {
      setRawBoolean(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public BooleanBox getBoolean(long index, BooleanBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, false);
          return box;
        }
      }
      box.fill(true, getRawBoolean(dataSegment, index));
      return box;
    }

    @Override
    public U1Vec withValidity(U1Vec validity) {
      requireNonNull(validity, "validity");
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U1Impl(dataSegment, impl(validity).dataSegment);
    }

    @Override
    public Stream<Boolean> allBooleans() {
      return LongStream.range(0, length()).mapToObj(this::getBoolean);
    }
  }

  record U8Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements VecImpl, U8Vec {
    static final VarHandle BYTE_HANDLE = ofSequence(ofValueBits(8, LITTLE_ENDIAN))
        .varHandle(byte.class, sequenceElement());

    @Override
    public void close() {
      try {
        CloseHelper.close(dataSegment);
      } finally {
        if (validitySegment != null) {
          CloseHelper.close(validitySegment);
        }
      }
    }

    @Override
    public long length() {
      return dataSegment.byteSize();
    }

    @Override
    public boolean isNull(long index) {
      if (validitySegment == null) {
        return false;
      }
      return !U1Impl.getRawBoolean(validitySegment, index);
    }

    @Override
    public void setNull(long index) {
      if (validitySegment == null) {
        throw doNotSupportNull();
      }
      BYTE_HANDLE.set(dataSegment, index, (byte) 0);
      U1Impl.setRawBoolean(validitySegment, index, false);
    }

    @Override
    public byte getByte(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return (byte) BYTE_HANDLE.get(dataSegment, index);
    }

    @Override
    public void setByte(long index, byte value) {
      BYTE_HANDLE.set(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public ByteBox getByte(long index, ByteBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, (byte) 0);
          return box;
        }
      }
      var value = (byte) BYTE_HANDLE.get(dataSegment, index);
      box.fill(true, value);
      return box;
    }

    @Override
    public U8Vec withValidity(U1Vec validity) {
      requireNonNull(validity, "validity");
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U8Impl(dataSegment, impl(validity).dataSegment);
    }

    @Override
    public IntStream allBytes() {
      return LongStream.range(0, length()).mapToInt(this::getByte);
    }
  }

  record U16Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements VecImpl, U16Vec {
    static final VarHandle SHORT_HANDLE = ofSequence(ofValueBits(16, LITTLE_ENDIAN))
        .varHandle(short.class, sequenceElement());
    static final VarHandle CHAR_HANDLE = ofSequence(ofValueBits(16, LITTLE_ENDIAN))
        .varHandle(char.class, sequenceElement());

    @Override
    public void close() {
      try {
        CloseHelper.close(dataSegment);
      } finally {
        if (validitySegment != null) {
          CloseHelper.close(validitySegment);
        }
      }
    }

    @Override
    public long length() {
      return dataSegment.byteSize() >> 1;
    }

    @Override
    public boolean isNull(long index) {
      if (validitySegment == null) {
        return false;
      }
      return !U1Impl.getRawBoolean(validitySegment, index);
    }

    @Override
    public void setNull(long index) {
      if (validitySegment == null) {
        throw doNotSupportNull();
      }
      SHORT_HANDLE.set(dataSegment, index, (short) 0);
      U1Impl.setRawBoolean(validitySegment, index, false);
    }

    @Override
    public short getShort(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return (short) SHORT_HANDLE.get(dataSegment, index);
    }

    @Override
    public void setShort(long index, short value) {
      SHORT_HANDLE.set(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public char getChar(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return (char) CHAR_HANDLE.get(dataSegment, index);
    }

    @Override
    public void setChar(long index, char value) {
      CHAR_HANDLE.set(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public ShortBox getShort(long index, ShortBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, (short) 0);
          return box;
        }
      }
      var value = (short) SHORT_HANDLE.get(dataSegment, index);
      box.fill(true, value);
      return box;
    }

    @Override
    public CharBox getChar(long index, CharBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, '\0');
          return box;
        }
      }
      var value = (char) CHAR_HANDLE.get(dataSegment, index);
      box.fill(true, value);
      return box;
    }

    @Override
    public U16Vec withValidity(U1Vec validity) {
      requireNonNull(validity, "validity");
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U16Impl(dataSegment, impl(validity).dataSegment);
    }

    @Override
    public IntStream allShorts() {
      return LongStream.range(0, length()).mapToInt(this::getShort);
    }

    @Override
    public IntStream allChars() {
      return LongStream.range(0, length()).mapToInt(this::getChar);
    }
  }

  record U32Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements VecImpl, U32Vec {
    static final VarHandle INT_HANDLE = ofSequence(ofValueBits(32, LITTLE_ENDIAN))
        .varHandle(int.class, sequenceElement());
    static final VarHandle FLOAT_HANDLE = ofSequence(ofValueBits(32, LITTLE_ENDIAN))
        .varHandle(float.class, sequenceElement());

    private static int getRawInt(MemorySegment array, long at) {
      return (int) INT_HANDLE.get(array, at);
    }

    @Override
    public void close() {
      try {
        CloseHelper.close(dataSegment);
      } finally {
        if (validitySegment != null) {
          CloseHelper.close(validitySegment);
        }
      }
    }

    @Override
    public long length() {
      return dataSegment.byteSize() >> 2;
    }

    @Override
    public boolean isNull(long index) {
      if (validitySegment == null) {
        return false;
      }
      return !U1Impl.getRawBoolean(validitySegment, index);
    }

    @Override
    public void setNull(long index) {
      if (validitySegment == null) {
        throw doNotSupportNull();
      }
      INT_HANDLE.set(dataSegment, index, 0);
      U1Impl.setRawBoolean(validitySegment, index, false);
    }

    @Override
    public int getInt(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return (int) INT_HANDLE.get(dataSegment, index);
    }

    @Override
    public void setInt(long index, int value) {
      INT_HANDLE.set(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public float getFloat(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return (float) FLOAT_HANDLE.get(dataSegment, index);
    }

    @Override
    public void setFloat(long index, float value) {
      FLOAT_HANDLE.set(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public IntBox getInt(long index, IntBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, 0);
          return box;
        }
      }
      var value = (int) INT_HANDLE.get(dataSegment, index);
      box.fill(true, value);
      return box;
    }

    @Override
    public FloatBox getFloat(long index, FloatBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, 0);
          return box;
        }
      }
      var value = (float) FLOAT_HANDLE.get(dataSegment, index);
      box.fill(true, value);
      return box;
    }

    @Override
    public U32Vec withValidity(U1Vec validity) {
      requireNonNull(validity, "validity");
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U32Impl(dataSegment, impl(validity).dataSegment);
    }

    @Override
    public IntStream allInts() {
      return LongStream.range(0, length()).mapToInt(this::getInt);
    }

    @Override
    public DoubleStream allFloats() {
      return LongStream.range(0, length()).mapToDouble(this::getFloat);
    }
  }

  record U64Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements VecImpl, U64Vec {
    static final VarHandle LONG_HANDLE = ofSequence(ofValueBits(64, LITTLE_ENDIAN))
        .varHandle(long.class, sequenceElement());
    static final VarHandle DOUBLE_HANDLE = ofSequence(ofValueBits(64, LITTLE_ENDIAN))
        .varHandle(double.class, sequenceElement());

    @Override
    public void close() {
      try {
        CloseHelper.close(dataSegment);
      } finally {
        if (validitySegment != null) {
          CloseHelper.close(validitySegment);
        }
      }
    }

    @Override
    public long length() {
      return dataSegment.byteSize() >> 3;
    }

    @Override
    public boolean isNull(long index) {
      if (validitySegment == null) {
        return false;
      }
      return !U1Impl.getRawBoolean(validitySegment, index);
    }

    @Override
    public void setNull(long index) {
      if (validitySegment == null) {
        throw doNotSupportNull();
      }
      LONG_HANDLE.set(dataSegment, index, 0L);
      U1Impl.setRawBoolean(validitySegment, index, false);
    }

    @Override
    public long getLong(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return (long) LONG_HANDLE.get(dataSegment, index);
    }

    @Override
    public void setLong(long index, long value) {
      LONG_HANDLE.set(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public double getDouble(long index) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          throw valueIsNull();
        }
      }
      return (double) DOUBLE_HANDLE.get(dataSegment, index);
    }

    @Override
    public void setDouble(long index, double value) {
      DOUBLE_HANDLE.set(dataSegment, index, value);
      if (validitySegment != null) {
        U1Impl.setRawBoolean(validitySegment, index, true);
      }
    }

    @Override
    public LongBox getLong(long index, LongBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, 0);
          return box;
        }
      }
      var value = (long) LONG_HANDLE.get(dataSegment, index);
      box.fill(true, value);
      return box;
    }

    @Override
    public DoubleBox getDouble(long index, DoubleBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, 0);
          return box;
        }
      }
      var value = (double) DOUBLE_HANDLE.get(dataSegment, index);
      box.fill(true, value);
      return box;
    }

    @Override
    public U64Vec withValidity(U1Vec validity) {
      requireNonNull(validity, "validity");
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U64Impl(dataSegment, impl(validity).dataSegment);
    }

    @Override
    public LongStream allLongs() {
      return LongStream.range(0, length()).map(this::getLong);
    }

    @Override
    public DoubleStream allDoubles() {
      return LongStream.range(0, length()).mapToDouble(this::getDouble);
    }
  }

  record ListImpl<D extends Vec>(D data, MemorySegment dataSegment, MemorySegment offsetSegment, MemorySegment validitySegment) implements VecImpl, ListVec<D> {
    @Override
    public void close() {
      try {
        CloseHelper.close(dataSegment);
      } finally {
        try {
          CloseHelper.close(offsetSegment);
        } finally {
          if (validitySegment != null) {
            CloseHelper.close(validitySegment);
          }
        }
      }
    }

    @Override
    public long length() {
      return (offsetSegment.byteSize() >> 2) - 1;
    }

    @Override
    public boolean isNull(long at) {
      if (validitySegment == null) {
        return false;
      }
      return !U1Impl.getRawBoolean(validitySegment, at);
    }

    @Override
    public void setNull(long at) {
      if (validitySegment == null) {
        throw doNotSupportNull();
      }
      U1Impl.setRawBoolean(validitySegment, at, false);
    }

    @Override
    public ValuesBox getValues(long index, ValuesBox box) {
      requireNonNull(box, "box");
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          box.fill(false, -1, -1);
          return box;
        }
      }
      var start = U32Impl.getRawInt(offsetSegment, index);
      var end = U32Impl.getRawInt(offsetSegment, index + 1);
      box.fill(true, start, end);
      return box;
    }

    @Override
    public String getString(long index) {
      if (data.getClass() != U16Impl.class) {
        throw new IllegalStateException("getString is only supported on U16Dataset");
      }
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          return null;
        }
      }
      var start = U32Impl.getRawInt(offsetSegment, index);
      var end = U32Impl.getRawInt(offsetSegment, index + 1);
      var length = end - start;
      var charArray = new char[length];
      MemorySegment.ofArray(charArray).copyFrom(dataSegment.asSlice((long) start << 1L, (long) length << 1L));
      return new String(charArray);
    }

    @Override
    public TextWrap getTextWrap(long index) {
      if (data.getClass() != U16Impl.class) {
        throw new IllegalStateException("getTextWrap is only supported on U16Dataset");
      }
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          return null;
        }
      }
      var start = U32Impl.getRawInt(offsetSegment, index);
      var end = U32Impl.getRawInt(offsetSegment, index + 1);
      var length = end - start;
      return new TextWrap(dataSegment, start, length);
    }

    @Override
    public ListVec<D> withValidity(U1Vec validity) {
      requireNonNull(validity, "validity");
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new ListImpl<>(data, dataSegment, offsetSegment, impl(validity).dataSegment);
    }

    @Override
    public Stream<TextWrap> allTextWraps() {
      return LongStream.range(0, length()).mapToObj(this::getTextWrap);
    }
  }

  record StructImpl(MemorySegment validitySegment, List<Vec> fields) implements VecImpl, StructVec {
    @Override
    public MemorySegment dataSegment() {
      throw new UnsupportedOperationException("list of struct are not supported yet");
    }

    @Override
    public void close() throws UncheckedIOException {
      for(var field: fields) {
        field.close();
      }
      if (validitySegment != null) {
        CloseHelper.close(validitySegment);
      }
    }

    @Override
    public long length() {
      return fields.stream().mapToLong(Vec::length).min().orElse(0);
    }

    @Override
    public boolean isNull(long at) {
      if (validitySegment == null) {
        return false;
      }
      return !U1Impl.getRawBoolean(validitySegment, at);
    }

    @Override
    public void setNull(long at) {
      if (validitySegment == null) {
        throw doNotSupportNull();
      }
      U1Impl.setRawBoolean(validitySegment, at, false);
    }

    @Override
    public StructVec withValidity(U1Vec validity) {
      requireNonNull(validity, "validity");
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return StructVec.from(validity, fields);
    }

    @Override
    public StructVec splice(int removeStart, int removeEnd, Vec... vecs) {
      Objects.checkFromToIndex(removeStart, removeEnd, fields.size());
      var list = new ArrayList<Vec>();
      for(var i = 0; i < removeStart; i++) {
        list.add(fields.get(i));
      }
      for(var vec: vecs) {
        requireNonNull(vec, "one of the Vec inside 'vecs' is null");
        if (vec.length() < length()) {
          throw new IllegalArgumentException("vec.length < length");
        }
        list.add(vec);
      }
      for(var i = removeEnd; i < fields.size(); i++) {
        list.add(fields.get(i));
      }
      return new StructImpl(validitySegment, List.copyOf(list));
    }
  }
}
