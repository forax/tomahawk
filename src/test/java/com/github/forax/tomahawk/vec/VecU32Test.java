package com.github.forax.tomahawk.vec;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jdk.incubator.foreign.MemorySegment;

@SuppressWarnings("static-method")
public class VecU32Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U32Vec>> provideIntVecs() {
    return Stream.of(
        length -> U32Vec.wrap(new int[(int) length]),
        length -> U32Vec.from(null, MemorySegment.allocateNative(length * 4))
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U32Vec>> provideFloatVecs() {
    return Stream.of(
        length -> U32Vec.wrap(new float[(int) length]),
        length -> U32Vec.from(null, MemorySegment.allocateNative(length * 4))
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U32Vec>> provideAllVecs() {
    return Stream.of(
        length -> U32Vec.wrap(new float[(int) length]),
        length -> U32Vec.wrap(new float[(int) length]),
        length -> U32Vec.from(null, MemorySegment.allocateNative(length * 4))
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllVecs")
  public void length(LongFunction<? extends U32Vec> factory) {
    assertAll(
        () -> assertEquals(13, factory.apply(13).length()),
        () -> assertEquals(42, factory.apply(42).length())
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllVecs")
  public void notNullableByDefault(LongFunction<? extends U32Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertFalse(vec.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> vec.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideIntVecs")
  public void getSetInts(LongFunction<? extends U32Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertEquals(0, vec.getInt(0));
      assertEquals(0, vec.getInt(3));
      vec.setInt(0, 42);
      vec.setInt(3, 56);
      assertEquals(42, vec.getInt(0));
      assertEquals(56, vec.getInt(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideIntVecs")
  public void getBoxInts(LongFunction<? extends U32Vec> factory) {
    try(var base = factory.apply(5);
        var vec = base.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setInt(1, 1324);
      vec.setNull(2);
      vec.setInt(3, 2768);

      var box = new IntBox();
      vec.getInt(0, box);
      assertFalse(box.validity);
      vec.getInt(1, box);
      assertTrue(box.validity);
      assertEquals(1324, box.value);
      vec.getInt(2, box);
      assertFalse(box.validity);
      vec.getInt(3, box);
      assertTrue(box.validity);
      assertEquals(2768, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideIntVecs")
  public void wrapOutOfBoundsInts(LongFunction<? extends U32Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getInt(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getInt(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setInt(7, 42)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setInt(-1, 42))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideIntVecs")
  public void validityInts(LongFunction<? extends U32Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var vec = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setInt(0, 42);
      vec.setInt(3, 56);
      vec.setNull(0);
      vec.setNull(3);
      assertAll(
          () -> assertTrue(vec.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> vec.getInt(0)),
          () -> vec.getInt(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(vec.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.getInt(3)),
          () -> vec.getInt(3, (validity, __) -> assertFalse(validity))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideFloatVecs")
  public void getSetFloats(LongFunction<? extends U32Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertEquals(0f, vec.getFloat(0));
      assertEquals(0f, vec.getFloat(3));
      vec.setFloat(0, 42f);
      vec.setFloat(3, 56f);
      assertEquals(42f, vec.getFloat(0));
      assertEquals(56f, vec.getFloat(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideFloatVecs")
  public void getBoxFloats(LongFunction<? extends U32Vec> factory) {
    try(var base = factory.apply(5);
        var vec = base.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setFloat(1, 1324f);
      vec.setNull(2);
      vec.setFloat(3, 2768f);

      var box = new FloatBox();
      vec.getFloat(0, box);
      assertFalse(box.validity);
      vec.getFloat(1, box);
      assertTrue(box.validity);
      assertEquals(1324f, box.value);
      vec.getFloat(2, box);
      assertFalse(box.validity);
      vec.getFloat(3, box);
      assertTrue(box.validity);
      assertEquals(2768f, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideFloatVecs")
  public void wrapOutOfBoundsFloats(LongFunction<? extends U32Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getFloat(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getFloat(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setFloat(7, 42f)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setFloat(-1, 42f))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideFloatVecs")
  public void validityFloats(LongFunction<? extends U32Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var vec = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setFloat(0, 42f);
      vec.setFloat(3, 56f);
      vec.setNull(0);
      vec.setNull(3);
      assertAll(
          () -> assertTrue(vec.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> vec.getFloat(0)),
          () -> vec.getFloat(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(vec.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.getFloat(3)),
          () -> vec.getFloat(3, (validity, __) -> assertFalse(validity))
      );
    }
  }

  @Test
  public void mapNew() throws IOException {
    var path = Files.createTempFile("map-new", "");
    Closeable andClean = () -> {
      Files.delete(path);
    };
    try(andClean) {
      var vec = U32Vec.mapNew(null, path, 128);
      assertEquals(128, vec.length());
    }
  }
}