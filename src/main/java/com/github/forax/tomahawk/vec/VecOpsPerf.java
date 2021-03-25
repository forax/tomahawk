package com.github.forax.tomahawk.vec;


import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteOrder;

public class VecOpsPerf {
  private static final VectorSpecies<Integer> SPECIES_U32 = IntVector.SPECIES_PREFERRED;

  public static void handCraftedInt(U32Vec vDest, U32Vec vec1, U32Vec vec2, VecOp.IntBinOp binaryOp) {
    var bufferDest = ((VecImpl.U32Impl) vDest).dataSegment().asByteBuffer();
    var buffer1 = ((VecImpl.U32Impl) vec1).dataSegment().asByteBuffer();
    var buffer2 = ((VecImpl.U32Impl) vec2).dataSegment().asByteBuffer();

    if (buffer1.capacity() != buffer2.capacity() ||
        buffer1.capacity() != bufferDest.capacity()) {
      throw new IllegalArgumentException("not the same length");
    }

    var sliceLength = SPECIES_U32.vectorByteSize();

    //System.err.println("slice1 " + slice1 + " slice2 " + slice2);

    var loopBound = bufferDest.capacity() - sliceLength;

    //System.err.println("i1LoopBound " + i1LoopBound + " i2LoopBound " + i2LoopBound);

    // main loop
    var i = 0;
    while (i < loopBound ) {
      var v1 = IntVector.fromByteBuffer(SPECIES_U32, buffer1, i, ByteOrder.LITTLE_ENDIAN);
      var v2 = IntVector.fromByteBuffer(SPECIES_U32, buffer2, i, ByteOrder.LITTLE_ENDIAN);

      //System.err.println("load v1 " + v1);
      //System.err.println("load v2 " + v2);

      var result = v1.add(v2);

      //System.err.println("result " + result);

      result.intoByteBuffer(bufferDest, i, ByteOrder.LITTLE_ENDIAN);

      i += sliceLength;
    }

    // post loop
    while (i < bufferDest.capacity()) {
      var v1 = buffer1.getInt(i);
      var v2 = buffer2.getInt(i);

      //System.err.println("plain load v1 " + v1);
      //System.err.println("plain load v2 " + v2);

      var result = v1 + v2;
      bufferDest.putInt(i, result);

      i += 4;
    }
  }
}
