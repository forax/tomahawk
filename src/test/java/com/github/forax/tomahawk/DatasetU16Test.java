package com.github.forax.tomahawk;

import com.github.forax.tomahawk.Tomahawk.CharBox;
import com.github.forax.tomahawk.Tomahawk.ShortBox;
import com.github.forax.tomahawk.Tomahawk.U16Dataset;
import com.github.forax.tomahawk.Tomahawk.U1Dataset;
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
public class DatasetU16Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U16Dataset>> provideShortDatasets() {
    return Stream.of(
        length -> U16Dataset.wrap(new short[(int) length]),
        length -> U16Dataset.from(MemorySegment.allocateNative(length * 2), null)
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U16Dataset>> provideCharDatasets() {
    return Stream.of(
        length -> U16Dataset.wrap(new char[(int) length]),
        length -> U16Dataset.from(MemorySegment.allocateNative(length * 2), null)
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U16Dataset>> provideAllDatasets() {
    return Stream.of(
        length -> U16Dataset.wrap(new short[(int) length]),
        length -> U16Dataset.wrap(new char[(int) length]),
        length -> U16Dataset.from(MemorySegment.allocateNative(length * 2), null)
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllDatasets")
  public void length(LongFunction<? extends U16Dataset> factory) {
    assertAll(
        () -> assertEquals(13, factory.apply(13).length()),
        () -> assertEquals(42, factory.apply(42).length())
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllDatasets")
  public void notNullableByDefault(LongFunction<? extends U16Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertFalse(dataset.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> dataset.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideShortDatasets")
  public void getSetBytes(LongFunction<? extends U16Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertEquals((short) 0, dataset.getShort(0));
      assertEquals(0, dataset.getShort(3));
      dataset.setShort(0, (short) 42);
      dataset.setShort(3, (short) 56);
      assertEquals((short) 42, dataset.getShort(0));
      assertEquals((short) 56, dataset.getShort(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideShortDatasets")
  public void getBoxBytes(LongFunction<? extends U16Dataset> factory) {
    try(var base = factory.apply(5);
        var dataset = base.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setShort(1, (short) 1324);
      dataset.setNull(2);
      dataset.setShort(3, (short) 2768);

      var box = new ShortBox();
      dataset.getShort(0, box);
      assertFalse(box.validity);
      dataset.getShort(1, box);
      assertTrue(box.validity);
      assertEquals((short) 1324, box.value);
      dataset.getShort(2, box);
      assertFalse(box.validity);
      dataset.getShort(3, box);
      assertTrue(box.validity);
      assertEquals((short) 2768, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideShortDatasets")
  public void wrapOutOfBoundsBytes(LongFunction<? extends U16Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getShort(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getShort(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setShort(7, (short) 42)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setShort(-1, (short) 42))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideShortDatasets")
  public void validityBytes(LongFunction<? extends U16Dataset> factory) {
    try(var simpleDataset = factory.apply(5);
        var dataset = simpleDataset.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setShort(0, (short) 42);
      dataset.setShort(3, (short) 56);
      dataset.setNull(0);
      dataset.setNull(3);
      assertAll(
          () -> assertTrue(dataset.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getShort(0)),
          () -> dataset.getShort(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getShort(3)),
          () -> dataset.getShort(3, (validity, __) -> assertFalse(validity))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideCharDatasets")
  public void getSetChars(LongFunction<? extends U16Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertEquals('\0', dataset.getChar(0));
      assertEquals('\0', dataset.getChar(3));
      dataset.setChar(0, 'A');
      dataset.setChar(3, 'z');
      assertEquals('A', dataset.getChar(0));
      assertEquals('z', dataset.getChar(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideCharDatasets")
  public void getBoxChars(LongFunction<? extends U16Dataset> factory) {
    try(var base = factory.apply(5);
        var dataset = base.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setChar(1, 'A');
      dataset.setNull(2);
      dataset.setChar(3, 'z');

      var box = new CharBox();
      dataset.getChar(0, box);
      assertFalse(box.validity);
      dataset.getChar(1, box);
      assertTrue(box.validity);
      assertEquals('A', box.value);
      dataset.getChar(2, box);
      assertFalse(box.validity);
      dataset.getChar(3, box);
      assertTrue(box.validity);
      assertEquals('z', box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideCharDatasets")
  public void wrapOutOfBoundsChars(LongFunction<? extends U16Dataset> factory) {
    try(var dataset = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getChar(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.getChar(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setChar(7, 'A')),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> dataset.setChar(-1, 'A'))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideCharDatasets")
  public void validityChars(LongFunction<? extends U16Dataset> factory) {
    try(var simpleDataset = factory.apply(5);
        var dataset = simpleDataset.withValidity(U1Dataset.wrap(new long[1]))) {
      dataset.setChar(0, 'A');
      dataset.setChar(3, 'z');
      dataset.setNull(0);
      dataset.setNull(3);
      assertAll(
          () -> assertTrue(dataset.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getChar(0)),
          () -> dataset.getChar(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getChar(3)),
          () -> dataset.getChar(3, (validity, __) -> assertFalse(validity))
      );
    }
  }
}