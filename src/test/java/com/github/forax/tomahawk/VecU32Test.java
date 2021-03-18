package com.github.forax.tomahawk;

import jdk.incubator.foreign.MemorySegment;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.LongFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("static-method")
public class VecU32Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U32Vec>> provideIntDatasets() {
    return Stream.of(
        length -> U32Vec.wrap(new int[(int) length]),
        length -> U32Vec.from(MemorySegment.allocateNative(length * 4), null)
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U32Vec>> provideFloatDatasets() {
    return Stream.of(
        length -> U32Vec.wrap(new float[(int) length]),
        length -> U32Vec.from(MemorySegment.allocateNative(length * 4), null)
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U32Vec>> provideAllDatasets() {
    return Stream.of(
        length -> U32Vec.wrap(new float[(int) length]),
        length -> U32Vec.wrap(new float[(int) length]),
        length -> U32Vec.from(MemorySegment.allocateNative(length * 4), null)
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllDatasets")
  public void length(LongFunction<? extends U32Vec> factory) {
    assertAll(
        () -> assertEquals(13, factory.apply(13).length()),
        () -> assertEquals(42, factory.apply(42).length())
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllDatasets")
  public void notNullableByDefault(LongFunction<? extends U32Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertFalse(dataset.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> dataset.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideIntDatasets")
  public void getSetInts(LongFunction<? extends U32Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertEquals(0, dataset.getInt(0));
      assertEquals(0, dataset.getInt(3));
      dataset.setInt(0, 42);
      dataset.setInt(3, 56);
      assertEquals(42, dataset.getInt(0));
      assertEquals(56, dataset.getInt(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideIntDatasets")
  public void getBoxInts(LongFunction<? extends U32Vec> factory) {
    try(var base = factory.apply(5);
        var dataset = base.withValidity(U1Vec.wrap(new long[1]))) {
      dataset.setInt(1, 1324);
      dataset.setNull(2);
      dataset.setInt(3, 2768);

      var box = new IntBox();
      dataset.getInt(0, box);
      assertFalse(box.validity);
      dataset.getInt(1, box);
      assertTrue(box.validity);
      assertEquals(1324, box.value);
      dataset.getInt(2, box);
      assertFalse(box.validity);
      dataset.getInt(3, box);
      assertTrue(box.validity);
      assertEquals(2768, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideIntDatasets")
  public void wrapOutOfBoundsInts(LongFunction<? extends U32Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getInt(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getInt(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setInt(7, 42)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setInt(-1, 42))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideIntDatasets")
  public void validityInts(LongFunction<? extends U32Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var dataset = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      dataset.setInt(0, 42);
      dataset.setInt(3, 56);
      dataset.setNull(0);
      dataset.setNull(3);
      assertAll(
          () -> assertTrue(dataset.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getInt(0)),
          () -> dataset.getInt(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getInt(3)),
          () -> dataset.getInt(3, (validity, __) -> assertFalse(validity))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideFloatDatasets")
  public void getSetFloats(LongFunction<? extends U32Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertEquals(0f, dataset.getFloat(0));
      assertEquals(0f, dataset.getFloat(3));
      dataset.setFloat(0, 42f);
      dataset.setFloat(3, 56f);
      assertEquals(42f, dataset.getFloat(0));
      assertEquals(56f, dataset.getFloat(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideFloatDatasets")
  public void getBoxFloats(LongFunction<? extends U32Vec> factory) {
    try(var base = factory.apply(5);
        var dataset = base.withValidity(U1Vec.wrap(new long[1]))) {
      dataset.setFloat(1, 1324f);
      dataset.setNull(2);
      dataset.setFloat(3, 2768f);

      var box = new FloatBox();
      dataset.getFloat(0, box);
      assertFalse(box.validity);
      dataset.getFloat(1, box);
      assertTrue(box.validity);
      assertEquals(1324f, box.value);
      dataset.getFloat(2, box);
      assertFalse(box.validity);
      dataset.getFloat(3, box);
      assertTrue(box.validity);
      assertEquals(2768f, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideFloatDatasets")
  public void wrapOutOfBoundsFloats(LongFunction<? extends U32Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getFloat(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getFloat(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setFloat(7, 42f)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setFloat(-1, 42f))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideFloatDatasets")
  public void validityFloats(LongFunction<? extends U32Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var dataset = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      dataset.setFloat(0, 42f);
      dataset.setFloat(3, 56f);
      dataset.setNull(0);
      dataset.setNull(3);
      assertAll(
          () -> assertTrue(dataset.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getFloat(0)),
          () -> dataset.getFloat(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getFloat(3)),
          () -> dataset.getFloat(3, (validity, __) -> assertFalse(validity))
      );
    }
  }
}