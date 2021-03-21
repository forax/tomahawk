package com.github.forax.tomahawk.vec;

import com.github.forax.tomahawk.vec.VecImpl.U16Impl;
import com.github.forax.tomahawk.vec.VecImpl.U32Impl;
import com.github.forax.tomahawk.vec.VecImpl.U8Impl;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;

import java.lang.invoke.MethodHandles.Lookup;
import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public interface VecOp {
  interface IntBinOp {
    int apply(int v1, int v2);
  }
  interface FloatBinOp {
    float apply(float v1, float v2);
  }
  interface LongBinOp {
    long apply(long v1, long v2);
  }
  interface DoubleBinOp {
    double apply(double v1, double v2);
  }

  <T> void applyInt(Vec dest, Vec vec1, Vec vec2, IntBinOp binaryOp);

  static VecOp create(Lookup lookup) {
    return new VecOp() {
      @Override
      public <T> void applyInt(Vec dest, Vec vec1, Vec vec2, IntBinOp op) {
        VecOp.doApplyInt(dest, vec1, vec2, op);
      }
    };
  }

  class ConstantInt32 {
    private static final VectorSpecies<Byte> U8_SPECIES;
    private static final VectorSpecies<Short> U16_SPECIES;
    private static final VectorSpecies<Integer> U32_SPECIES;

    static {
      var shape = VectorShape.preferredShape();
      var bits = shape.vectorBitSize();
      U8_SPECIES = VectorSpecies.of(byte.class, VectorShape.forBitSize(Math.max(64, bits >> 2)));
      U16_SPECIES = VectorSpecies.of(short.class, VectorShape.forBitSize(Math.max(64, bits >> 1)));
      U32_SPECIES = VectorSpecies.of(int.class, shape);
    }
  }

  private static void doApplyInt(Vec vDest, Vec vec1, Vec vec2, IntBinOp binaryOp) {
    var bufferDest = byteBuffer(vDest);
    var buffer1 = byteBuffer(vec1);
    var buffer2 = byteBuffer(vec2);

    var sliceLength = ConstantInt32.U32_SPECIES.vectorByteSize();
    var sliceDest = sliceLength >> shift(vDest);
    var slice1 = sliceLength >> shift(vec1);
    var slice2 = sliceLength >> shift(vec2);

    //System.err.println("slice1 " + slice1 + " slice2 " + slice2);

    var iDestLoopBounds = bufferDest.capacity() - vectorByteSize(vDest);
    var i1LoopBounds = buffer1.capacity() - vectorByteSize(vec1);
    var i2LoopBounds = buffer2.capacity() - vectorByteSize(vec2);

    //System.err.println("i1LoopBounds " + i1LoopBounds + " i2LoopBounds " + i2LoopBounds);

    // main loop
    var iDest = 0; var i1 = 0; var i2 = 0;
    while (iDest < iDestLoopBounds && i1 < i1LoopBounds && i2 < i2LoopBounds) {
      var v1 = loadInt(vec1, buffer1, i1);
      var v2 = loadInt(vec2, buffer2, i2);

      //System.err.println("load v1 " + v1);
      //System.err.println("load v2 " + v2);

      var result = vecOp(v1, v2);

      //System.err.println("result " + result);

      storeInt(vDest, bufferDest, iDest, result);

      iDest += sliceDest;
      i1 += slice1;
      i2 += slice2;
    }

    // post loop
    while(iDest < bufferDest.capacity() && i1 < buffer1.capacity() && i2 < buffer2.capacity()) {
      var v1 = plainLoadInt(vec1, buffer1, i1);
      var v2 = plainLoadInt(vec2, buffer2, i2);

      //System.err.println("plain load v1 " + v1);
      //System.err.println("plain load v2 " + v2);

      var result = plainOp(v1, v2);
      plainStoreInt(vDest, bufferDest, iDest, result);

      iDest += 4 >> shift(vDest);
      i1 += 4 >> shift(vec1);
      i2 += 4 >> shift(vec2);
    }
  }

  private static IntVector vecOp(IntVector v1, IntVector v2) {
    return v1.add(v2);
  }

  private static int plainOp(int v1, int v2) {
    return v1 + v2;
  }

  private static int shift(Vec vec) {
    if (vec instanceof U8Impl u8Impl) {
      return 2;
    }
    if (vec instanceof U16Impl u16Impl) {
      return 1;
    }
    if (vec instanceof U32Impl u32Impl) {
      return 0;
    }
    throw new AssertionError();
  }

  private static int vectorByteSize(Vec vec) {
    if (vec instanceof U8Impl u8Impl) {
      return ConstantInt32.U8_SPECIES.vectorByteSize();
    }
    if (vec instanceof U16Impl u16Impl) {
      return ConstantInt32.U16_SPECIES.vectorByteSize();
    }
    if (vec instanceof U32Impl u32Impl) {
      return ConstantInt32.U32_SPECIES.vectorByteSize();
    }
    throw new AssertionError();
  }

  private static ByteBuffer byteBuffer(Vec vec) {
    if (vec instanceof U8Impl u8Impl) {
      return u8Impl.dataSegment().asByteBuffer().order(LITTLE_ENDIAN);
    }
    if (vec instanceof U16Impl u16Impl) {
      return u16Impl.dataSegment().asByteBuffer().order(LITTLE_ENDIAN);
    }
    if (vec instanceof U32Impl u32Impl) {
      return u32Impl.dataSegment().asByteBuffer().order(LITTLE_ENDIAN);
    }
    throw new AssertionError();
  }

  private static IntVector loadInt(Vec vec, ByteBuffer buffer, int i) {
    if (vec instanceof U8Impl) {
      var bv = ByteVector.fromByteBuffer(ConstantInt32.U8_SPECIES, buffer, i, LITTLE_ENDIAN);
      return (IntVector) bv.convertShape(VectorOperators.B2I, ConstantInt32.U32_SPECIES, 0);
    }
    if (vec instanceof U16Impl) {
      var sv = ShortVector.fromByteBuffer(ConstantInt32.U16_SPECIES, buffer, i, LITTLE_ENDIAN);
      return (IntVector) sv.convertShape(VectorOperators.S2I, ConstantInt32.U32_SPECIES, 0);
    }
    if (vec instanceof U32Impl) {
      return IntVector.fromByteBuffer(ConstantInt32.U32_SPECIES, buffer, i, LITTLE_ENDIAN);
    }
    throw new AssertionError();
  }

  private static void storeInt(Vec vec, ByteBuffer buffer, int i, IntVector result) {
    if (vec instanceof U8Impl) {
      var bv = (ByteVector) result.convertShape(VectorOperators.I2B, ConstantInt32.U8_SPECIES, 0);
      bv.intoByteBuffer(buffer, i, LITTLE_ENDIAN);
      return;
    }
    if (vec instanceof U16Impl) {
      var sv = (ShortVector) result.convertShape(VectorOperators.I2S, ConstantInt32.U16_SPECIES, 0);
      sv.intoByteBuffer(buffer, i, LITTLE_ENDIAN);
      return;
    }
    if (vec instanceof U32Impl) {
      result.intoByteBuffer(buffer, i, LITTLE_ENDIAN);
      return;
    }
    throw new AssertionError();
  }

  private static int plainLoadInt(Vec vec, ByteBuffer buffer, int i) {
    if (vec instanceof U8Impl) {
      return buffer.get(i);
    }
    if (vec instanceof U16Impl) {
      return buffer.getShort(i);
    }
    if (vec instanceof U32Impl) {
      return buffer.getInt(i);
    }
    throw new AssertionError();
  }

  private static void plainStoreInt(Vec vec, ByteBuffer buffer, int i, int result) {
    if (vec instanceof U8Impl) {
      buffer.put(i, (byte) result);
      return;
    }
    if (vec instanceof U16Impl) {
      buffer.putShort(i, (short) result);
      return;
    }
    if (vec instanceof U32Impl) {
      buffer.putInt(i, result);
      return;
    }
    throw new AssertionError();
  }
}
