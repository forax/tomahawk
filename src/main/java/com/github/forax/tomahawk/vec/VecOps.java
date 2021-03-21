package com.github.forax.tomahawk.vec;

import com.github.forax.tomahawk.vec.VecOp.IntBinOp;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.ByteBuffer;

import static java.lang.invoke.MethodType.methodType;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

interface VecOps {
  record VecOpImpl(MethodHandle applyInt, MethodHandle applyFloat, MethodHandle applyLong, MethodHandle applyDouble) implements VecOp {
    @Override
    public void applyInt(Vec dest, Vec vec1, Vec vec2, IntBinOp binaryOp) {
      try {
        applyInt.invokeExact(dest, vec1, vec2, binaryOp);
      } catch (RuntimeException | Error e) {
        throw e;
      } catch(Throwable t) {
        throw new UndeclaredThrowableException(t);
      }
    }

    @Override
    public void applyFloat(Vec dest, Vec vec1, Vec vec2, FloatBinOp binaryOp) {
      throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void applyLong(Vec dest, Vec vec1, Vec vec2, LongBinOp binaryOp) {
      throw new UnsupportedOperationException("NYI");
    }

    @Override
    public void applyDouble(Vec dest, Vec vec1, Vec vec2, LongBinOp binaryOp) {
      throw new UnsupportedOperationException("NYI");
    }
  }

  static VecOp create(Lookup lookup) {
    var applyInt = new IntSpecies.IntInliningCache().dynamicInvoker();
    return new VecOpImpl(applyInt, null, null, null);
  }


  class IntSpecies {
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

    static class IntInliningCache extends MutableCallSite {
      private static final MethodHandle FALLBACK, DO_APPLY_INT, TYPE_CHECK;
      static {
        try {
          var lookup = MethodHandles.lookup();
          FALLBACK = lookup.findVirtual(IntInliningCache.class, "fallback",
              methodType(void.class, Vec.class, Vec.class, Vec.class, IntBinOp.class));
          DO_APPLY_INT = lookup.findStatic(IntSpecies.class, "doApplyInt",
              methodType(void.class, Vec.class, Vec.class, Vec.class, IntBinOp.class));
          TYPE_CHECK = lookup.findStatic(IntInliningCache.class, "typeCheck",
              methodType(boolean.class, Vec.class, Vec.class, Vec.class, Class.class, Class.class, Class.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
          throw new AssertionError(e);
        }
      }

      public IntInliningCache() {
        super(methodType(void.class, Vec.class, Vec.class, Vec.class, IntBinOp.class));
        setTarget(FALLBACK.bindTo(this));
      }

      private static boolean typeCheck(Vec dest, Vec vec1, Vec vec2, Class<?> typeDest, Class<?> typeVec1, Class<?> typeVec2) {
        return dest.getClass() == typeDest && vec1.getClass() == typeVec1 && vec2.getClass() == typeVec2;
      }

      private void fallback(Vec dest, Vec vec1, Vec vec2, IntBinOp binaryOp) {
        Class<?> typeDest = dest.getClass();
        Class<?> typeVec1 = vec1.getClass();
        Class<?> typeVec2 = vec2.getClass();

        var target = DO_APPLY_INT
            .asType(methodType(void.class, typeDest, typeVec1, typeVec2, IntBinOp.class))  // please JIT !
            .asType(type());

        var guard = MethodHandles.guardWithTest(
            MethodHandles.insertArguments(TYPE_CHECK, 3, typeDest, typeVec1, typeVec2),
            target,
            new IntInliningCache().dynamicInvoker()
        );
        setTarget(guard);

        IntSpecies.doApplyInt(dest, vec1, vec2, binaryOp);
      }
    }

    private static void doApplyInt(Vec vDest, Vec vec1, Vec vec2, IntBinOp binaryOp) {
      var bufferDest = byteBuffer(vDest);
      var buffer1 = byteBuffer(vec1);
      var buffer2 = byteBuffer(vec2);

      var sliceLength = IntSpecies.U32_SPECIES.vectorByteSize();
      var sliceDest = sliceLength >> shift(vDest);
      var slice1 = sliceLength >> shift(vec1);
      var slice2 = sliceLength >> shift(vec2);

      //System.err.println("slice1 " + slice1 + " slice2 " + slice2);

      var iDestLoopBounds = bufferDest.capacity() - vectorByteSize(vDest);
      var i1LoopBounds = buffer1.capacity() - vectorByteSize(vec1);
      var i2LoopBounds = buffer2.capacity() - vectorByteSize(vec2);

      //System.err.println("i1LoopBounds " + i1LoopBounds + " i2LoopBounds " + i2LoopBounds);

      // main loop
      var iDest = 0;
      var i1 = 0;
      var i2 = 0;
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
      while (iDest < bufferDest.capacity() && i1 < buffer1.capacity() && i2 < buffer2.capacity()) {
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
      if (vec instanceof VecImpl.U8Impl) {
        return 2;
      }
      if (vec instanceof VecImpl.U16Impl) {
        return 1;
      }
      if (vec instanceof VecImpl.U32Impl) {
        return 0;
      }
      throw new AssertionError();
    }

    private static int vectorByteSize(Vec vec) {
      if (vec instanceof VecImpl.U8Impl) {
        return U8_SPECIES.vectorByteSize();
      }
      if (vec instanceof VecImpl.U16Impl) {
        return U16_SPECIES.vectorByteSize();
      }
      if (vec instanceof VecImpl.U32Impl) {
        return U32_SPECIES.vectorByteSize();
      }
      throw new AssertionError();
    }

    private static ByteBuffer byteBuffer(Vec vec) {
      if (vec instanceof VecImpl.U8Impl u8Impl) {
        return u8Impl.dataSegment().asByteBuffer().order(LITTLE_ENDIAN);
      }
      if (vec instanceof VecImpl.U16Impl u16Impl) {
        return u16Impl.dataSegment().asByteBuffer().order(LITTLE_ENDIAN);
      }
      if (vec instanceof VecImpl.U32Impl u32Impl) {
        return u32Impl.dataSegment().asByteBuffer().order(LITTLE_ENDIAN);
      }
      throw new AssertionError();
    }

    private static IntVector loadInt(Vec vec, ByteBuffer buffer, int i) {
      if (vec instanceof VecImpl.U8Impl) {
        var bv = ByteVector.fromByteBuffer(U8_SPECIES, buffer, i, LITTLE_ENDIAN);
        return (IntVector) bv.convertShape(VectorOperators.B2I, U32_SPECIES, 0);
      }
      if (vec instanceof VecImpl.U16Impl) {
        var sv = ShortVector.fromByteBuffer(U16_SPECIES, buffer, i, LITTLE_ENDIAN);
        return (IntVector) sv.convertShape(VectorOperators.S2I, U32_SPECIES, 0);
      }
      if (vec instanceof VecImpl.U32Impl) {
        return IntVector.fromByteBuffer(U32_SPECIES, buffer, i, LITTLE_ENDIAN);
      }
      throw new AssertionError();
    }

    private static void storeInt(Vec vec, ByteBuffer buffer, int i, IntVector result) {
      if (vec instanceof VecImpl.U8Impl) {
        var bv = (ByteVector) result.convertShape(VectorOperators.I2B, U8_SPECIES, 0);
        bv.intoByteBuffer(buffer, i, LITTLE_ENDIAN);
        return;
      }
      if (vec instanceof VecImpl.U16Impl) {
        var sv = (ShortVector) result.convertShape(VectorOperators.I2S, U16_SPECIES, 0);
        sv.intoByteBuffer(buffer, i, LITTLE_ENDIAN);
        return;
      }
      if (vec instanceof VecImpl.U32Impl) {
        result.intoByteBuffer(buffer, i, LITTLE_ENDIAN);
        return;
      }
      throw new AssertionError();
    }

    private static int plainLoadInt(Vec vec, ByteBuffer buffer, int i) {
      if (vec instanceof VecImpl.U8Impl) {
        return buffer.get(i);
      }
      if (vec instanceof VecImpl.U16Impl) {
        return buffer.getShort(i);
      }
      if (vec instanceof VecImpl.U32Impl) {
        return buffer.getInt(i);
      }
      throw new AssertionError();
    }

    private static void plainStoreInt(Vec vec, ByteBuffer buffer, int i, int result) {
      if (vec instanceof VecImpl.U8Impl) {
        buffer.put(i, (byte) result);
        return;
      }
      if (vec instanceof VecImpl.U16Impl) {
        buffer.putShort(i, (short) result);
        return;
      }
      if (vec instanceof VecImpl.U32Impl) {
        buffer.putInt(i, result);
        return;
      }
      throw new AssertionError();
    }
  }
}
