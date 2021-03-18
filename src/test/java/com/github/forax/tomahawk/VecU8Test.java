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
public class VecU8Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U8Vec>> provideByteDatasets() {
    return Stream.of(
        length -> U8Vec.wrap(new byte[(int) length]),
        length -> U8Vec.from(MemorySegment.allocateNative(length), null)
    );
  }

  @ParameterizedTest
  @MethodSource("provideByteDatasets")
  public void length(LongFunction<? extends U8Vec> factory) {
    assertAll(
        () -> assertEquals(13, factory.apply(13).length()),
        () -> assertEquals(42, factory.apply(42).length())
    );
  }

  @ParameterizedTest
  @MethodSource("provideByteDatasets")
  public void notNullableByDefault(LongFunction<? extends U8Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertFalse(dataset.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> dataset.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideByteDatasets")
  public void getSetBytes(LongFunction<? extends U8Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertEquals(0, dataset.getByte(0));
      assertEquals(0, dataset.getByte(3));
      dataset.setByte(0, (byte) 42);
      dataset.setByte(3, (byte) 56);
      assertEquals(42, dataset.getByte(0));
      assertEquals(56, dataset.getByte(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideByteDatasets")
  public void getBoxBytes(LongFunction<? extends U8Vec> factory) {
    try(var base = factory.apply(5);
        var dataset = base.withValidity(U1Vec.wrap(new long[1]))) {
      dataset.setByte(1, (byte) 124);
      dataset.setNull(2);
      dataset.setByte(3, (byte) -68);

      var box = new ByteBox();
      dataset.getByte(0, box);
      assertFalse(box.validity);
      dataset.getByte(1, box);
      assertTrue(box.validity);
      assertEquals((byte) 124, box.value);
      dataset.getByte(2, box);
      assertFalse(box.validity);
      dataset.getByte(3, box);
      assertTrue(box.validity);
      assertEquals((byte) -68, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideByteDatasets")
  public void wrapOutOfBoundsInts(LongFunction<? extends U8Vec> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getByte(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getByte(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setByte(7, (byte) 42)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setByte(-1, (byte) 42))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideByteDatasets")
  public void validityBytes(LongFunction<? extends U8Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var dataset = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      dataset.setByte(0, (byte) 42);
      dataset.setByte(3, (byte) 56);
      dataset.setNull(0);
      dataset.setNull(3);
      assertAll(
          () -> assertTrue(dataset.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getByte(0)),
          () -> dataset.getByte(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getByte(3)),
          () -> dataset.getByte(3, (validity, __) -> assertFalse(validity))
      );
    }
  }
}