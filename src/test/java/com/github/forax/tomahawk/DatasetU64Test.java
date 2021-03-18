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
public class DatasetU64Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U64Dataset>> provideLongDatasets() {
    return Stream.of(
        length -> U64Dataset.wrap(new long[(int) length]),
        length -> U64Dataset.from(MemorySegment.allocateNative(length * 8), null)
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U64Dataset>> provideDoubleDatasets() {
    return Stream.of(
        length -> U64Dataset.wrap(new double[(int) length]),
        length -> U64Dataset.from(MemorySegment.allocateNative(length * 8), null)
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U64Dataset>> provideAllDatasets() {
    return Stream.of(
        length -> U64Dataset.wrap(new long[(int) length]),
        length -> U64Dataset.wrap(new double[(int) length]),
        length -> U64Dataset.from(MemorySegment.allocateNative(length * 8), null)
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllDatasets")
  public void length(LongFunction<? extends U64Dataset> factory) {
    assertAll(
        () -> assertEquals(13, factory.apply(13).length()),
        () -> assertEquals(42, factory.apply(42).length())
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllDatasets")
  public void notNullableByDefault(LongFunction<? extends U64Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertFalse(dataset.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> dataset.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideLongDatasets")
  public void getSetLongs(LongFunction<? extends U64Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertEquals(0, dataset.getLong(0));
      assertEquals(0, dataset.getLong(3));
      dataset.setLong(0, 42L);
      dataset.setLong(3, 56L);
      assertEquals(42L, dataset.getLong(0));
      assertEquals(56L, dataset.getLong(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideLongDatasets")
  public void getBoxLongs(LongFunction<? extends U64Dataset> factory) {
    try(var base = factory.apply(5);
        var dataset = base.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setLong(1, 1324L);
      dataset.setNull(2);
      dataset.setLong(3, 2768);

      var box = new LongBox();
      dataset.getLong(0, box);
      assertFalse(box.validity);
      dataset.getLong(1, box);
      assertTrue(box.validity);
      assertEquals(1324L, box.value);
      dataset.getLong(2, box);
      assertFalse(box.validity);
      dataset.getLong(3, box);
      assertTrue(box.validity);
      assertEquals(2768L, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideLongDatasets")
  public void wrapOutOfBoundsLongs(LongFunction<? extends U64Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getLong(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getLong(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setLong(7, 42L)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setLong(-1, 42L))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideLongDatasets")
  public void validityLongs(LongFunction<? extends U64Dataset> factory) {
    try(var simpleDataset = factory.apply(5);
        var dataset = simpleDataset.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setLong(0, 42L);
      dataset.setLong(3, 56L);
      dataset.setNull(0);
      dataset.setNull(3);
      assertAll(
          () -> assertTrue(dataset.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getLong(0)),
          () -> dataset.getLong(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getLong(3)),
          () -> dataset.getLong(3, (validity, __) -> assertFalse(validity))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideDoubleDatasets")
  public void getSetDoubles(LongFunction<? extends U64Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertEquals(0.0, dataset.getDouble(0));
      assertEquals(0.0, dataset.getDouble(3));
      dataset.setDouble(0, 42.0);
      dataset.setDouble(3, 56.0);
      assertEquals(42.0, dataset.getDouble(0));
      assertEquals(56.0, dataset.getDouble(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideDoubleDatasets")
  public void getBoxDoubles(LongFunction<? extends U64Dataset> factory) {
    try(var base = factory.apply(5);
        var dataset = base.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setDouble(1, 1324.0);
      dataset.setNull(2);
      dataset.setDouble(3, 2768.0);

      var box = new DoubleBox();
      dataset.getDouble(0, box);
      assertFalse(box.validity);
      dataset.getDouble(1, box);
      assertTrue(box.validity);
      assertEquals(1324.0, box.value);
      dataset.getDouble(2, box);
      assertFalse(box.validity);
      dataset.getDouble(3, box);
      assertTrue(box.validity);
      assertEquals(2768.0, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideDoubleDatasets")
  public void wrapOutOfBoundsDoubles(LongFunction<? extends U64Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getDouble(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getDouble(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setDouble(7, 42.0)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setDouble(-1, 42.0))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideDoubleDatasets")
  public void validityDoubles(LongFunction<? extends U64Dataset> factory) {
    try(var simpleDataset = factory.apply(5);
        var dataset = simpleDataset.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setDouble(0, 42.0);
      dataset.setDouble(3, 56.0);
      dataset.setNull(0);
      dataset.setNull(3);
      assertAll(
          () -> assertTrue(dataset.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getDouble(0)),
          () -> dataset.getDouble(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getDouble(3)),
          () -> dataset.getDouble(3, (validity, __) -> assertFalse(validity))
      );
    }
  }
}