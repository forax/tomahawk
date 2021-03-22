package com.github.forax.tomahawk.vec;

import java.lang.invoke.MethodHandles.Lookup;

/**
 * TODO
 */
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

  void applyInt(Vec dest, Vec vec1, Vec vec2, IntBinOp binaryOp);
  void applyFloat(Vec dest, Vec vec1, Vec vec2, FloatBinOp binaryOp);
  void applyLong(Vec dest, Vec vec1, Vec vec2, LongBinOp binaryOp);
  void applyDouble(Vec dest, Vec vec1, Vec vec2, LongBinOp binaryOp);

  static VecOp of(Lookup lookup) {
    return VecOps.create(lookup);
  }
}
