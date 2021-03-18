package com.github.forax.tomahawk;

import com.github.forax.tomahawk.Tomahawk.*;
import jdk.incubator.foreign.MemorySegment;

import java.io.UncheckedIOException;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.incubator.foreign.MemoryLayout.PathElement.sequenceElement;
import static jdk.incubator.foreign.MemoryLayout.ofSequence;
import static jdk.incubator.foreign.MemoryLayout.ofValueBits;

interface DatasetImpl {
  private static IllegalStateException doNotSupportNull() {
    throw new IllegalStateException("this dataset do not support null");
  }
  static NullPointerException valueIsNull() {
    throw new NullPointerException("the value is null");
  }
  private static IllegalArgumentException invalidLength(Dataset self, Dataset validity) {
    throw new IllegalArgumentException("invalid length: length " + self.length() + " != validitySegment length " + validity.length());
  }

  MemorySegment dataSegment();

  static DatasetImpl impl(Dataset dataset) {
    return (DatasetImpl) dataset;
  }
  static U1Impl impl(U1Dataset dataset) {
    return (U1Impl) dataset;
  }
  static U16Impl impl(U16Dataset dataset) {
    return (U16Impl) dataset;
  }
  static U32Impl impl(U32Dataset dataset) {
    return (U32Impl) dataset;
  }

  static MemorySegment implDataOrNull(U1Dataset validity) {
    if (validity == null) {
      return null;
    }
    return impl(validity).dataSegment;
  }

  record U1Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements DatasetImpl, U1Dataset {
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
        if (dataSegment.isAlive()) {
          dataSegment.close();
        }
      } finally {
        if (validitySegment != null && validitySegment.isAlive()) {
          validitySegment.close();
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
      return U1Impl.getRawBoolean(validitySegment, index);
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
    public void getBoolean(long index, BooleanExtractor extractor) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          extractor.consume(false, false);
          return;
        }
      }
      extractor.consume(true, getRawBoolean(dataSegment, index));
    }

    @Override
    public U1Dataset withValidity(U1Dataset validity) {
      Objects.requireNonNull(validity);
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U1Impl(dataSegment, impl(validity).dataSegment);
    }
  }

  record U16Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements DatasetImpl, U16Dataset {
    static final VarHandle SHORT_HANDLE = ofSequence(ofValueBits(16, LITTLE_ENDIAN))
        .varHandle(short.class, sequenceElement());
    static final VarHandle CHAR_HANDLE = ofSequence(ofValueBits(16, LITTLE_ENDIAN))
        .varHandle(char.class, sequenceElement());

    @Override
    public void close() {
      try {
        if (dataSegment.isAlive()) {
          dataSegment.close();
        }
      } finally {
        if (validitySegment != null && validitySegment.isAlive()) {
          validitySegment.close();
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
      SHORT_HANDLE.set(dataSegment, index, 0);
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
    public void getShort(long index, ShortExtractor extractor) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          extractor.consume(false, (short) 0);
          return;
        }
      }
      var value = (short) SHORT_HANDLE.get(dataSegment, index);
      extractor.consume(true, value);
    }

    @Override
    public void getChar(long index, CharExtractor extractor) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          extractor.consume(false, '\0');
          return;
        }
      }
      var value = (char) CHAR_HANDLE.get(dataSegment, index);
      extractor.consume(true, value);
    }

    @Override
    public U16Dataset withValidity(U1Dataset validity) {
      Objects.requireNonNull(validity);
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U16Impl(dataSegment, impl(validity).dataSegment);
    }
  }

  record U32Impl(MemorySegment dataSegment, MemorySegment validitySegment) implements DatasetImpl, U32Dataset {
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
        if (dataSegment.isAlive()) {
          dataSegment.close();
        }
      } finally {
        if (validitySegment != null && validitySegment.isAlive()) {
          validitySegment.close();
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
    public void getInt(long index, IntExtractor extractor) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          extractor.consume(false, 0);
          return;
        }
      }
      var value = (int) INT_HANDLE.get(dataSegment, index);
      extractor.consume(true, value);
    }

    @Override
    public void getFloat(long index, FloatExtractor extractor) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          extractor.consume(false, 0);
          return;
        }
      }
      var value = (float) FLOAT_HANDLE.get(dataSegment, index);
      extractor.consume(true, value);
    }

    @Override
    public U32Dataset withValidity(U1Dataset validity) {
      Objects.requireNonNull(validity);
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new U32Impl(dataSegment, impl(validity).dataSegment);
    }
  }

  record ListImpl<D extends Dataset>(D data, MemorySegment dataSegment, MemorySegment offsetSegment, MemorySegment validitySegment) implements DatasetImpl, ListDataset<D> {
    @Override
    public void close() {
      try {
        if (dataSegment.isAlive()) {
          dataSegment.close();
        }
      } finally {
        try {
          if (offsetSegment.isAlive()) {
            offsetSegment.close();
          }
        } finally {
          if (validitySegment != null && validitySegment.isAlive()) {
            validitySegment.close();
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
    public void getValues(long index, ValuesExtractor extractor) {
      if (validitySegment != null) {
        if (!U1Impl.getRawBoolean(validitySegment, index)) {
          extractor.consume(false, -1, -1);
          return;
        }
      }
      var start = U32Impl.getRawInt(offsetSegment, index);
      var end = U32Impl.getRawInt(offsetSegment, index + 1);
      extractor.consume(true, start, end);
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
    public ListDataset<D> withValidity(U1Dataset validity) {
      Objects.requireNonNull(validity);
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return new ListImpl<>(data, dataSegment, offsetSegment, impl(validity).dataSegment);
    }
  }

  record StructImpl(MemorySegment validitySegment, List<Dataset> fields) implements DatasetImpl, StructDataset {
    @Override
    public MemorySegment dataSegment() {
      throw new UnsupportedOperationException("list of struct are not supported yet");
    }

    @Override
    public void close() throws UncheckedIOException {
      for(var field: fields) {
        field.close();
      }
      if (validitySegment != null && validitySegment.isAlive()) {
        validitySegment.close();
      }
    }

    @Override
    public long length() {
      return fields.stream().mapToLong(Dataset::length).min().orElse(0);
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
    public StructDataset withValidity(U1Dataset validity) {
      Objects.requireNonNull(validity);
      if (length() > validity.length()) {
        throw invalidLength(this, validity);
      }
      return StructDataset.from(validity, fields);
    }
  }
}
