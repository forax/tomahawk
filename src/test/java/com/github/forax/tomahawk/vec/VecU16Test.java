package com.github.forax.tomahawk.vec;

import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jdk.incubator.foreign.MemorySegment;

@SuppressWarnings("static-method")
public class VecU16Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U16Vec>> provideShortVecs() {
    return Stream.of(
        length -> U16Vec.wrap(new short[(int) length]),
        length -> U16Vec.from(null, MemorySegment.allocateNative(length * 2))
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U16Vec>> provideCharVecs() {
    return Stream.of(
        length -> U16Vec.wrap(new char[(int) length]),
        length -> U16Vec.from(null, MemorySegment.allocateNative(length * 2))
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U16Vec>> provideAllVecs() {
    return Stream.of(
        length -> U16Vec.wrap(new short[(int) length]),
        length -> U16Vec.wrap(new char[(int) length]),
        length -> U16Vec.from(null, MemorySegment.allocateNative(length * 2))
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllVecs")
  public void length(LongFunction<? extends U16Vec> factory) {
    assertAll(
        () -> {
          try (var vec = factory.apply(13)) {
            assertEquals(13, vec.length());
          }
        },
        () -> {
          try(var vec = factory.apply(42)) {
            assertEquals(42, vec.length());
          }
        }
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllVecs")
  public void notNullableByDefault(LongFunction<? extends U16Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertFalse(vec.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> vec.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideShortVecs")
  public void getSetBytes(LongFunction<? extends U16Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertEquals((short) 0, vec.getShort(0));
      assertEquals(0, vec.getShort(3));
      vec.setShort(0, (short) 42);
      vec.setShort(3, (short) 56);
      assertEquals((short) 42, vec.getShort(0));
      assertEquals((short) 56, vec.getShort(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideShortVecs")
  public void getBoxBytes(LongFunction<? extends U16Vec> factory) {
    try(var base = factory.apply(5);
        var vec = base.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setShort(1, (short) 1324);
      vec.setNull(2);
      vec.setShort(3, (short) 2768);

      var box = new ShortBox();
      vec.getShort(0, box);
      assertFalse(box.validity);
      vec.getShort(1, box);
      assertTrue(box.validity);
      assertEquals((short) 1324, box.value);
      vec.getShort(2, box);
      assertFalse(box.validity);
      vec.getShort(3, box);
      assertTrue(box.validity);
      assertEquals((short) 2768, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideShortVecs")
  public void wrapOutOfBoundsBytes(LongFunction<? extends U16Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getShort(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getShort(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setShort(7, (short) 42)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setShort(-1, (short) 42))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideShortVecs")
  public void validityBytes(LongFunction<? extends U16Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var vec = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setShort(0, (short) 42);
      vec.setShort(3, (short) 56);
      vec.setNull(0);
      vec.setNull(3);
      assertAll(
          () -> assertTrue(vec.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> vec.getShort(0)),
          () -> assertFalse(vec.getShort(0, new ShortBox()).validity),

          () -> assertTrue(vec.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.getShort(3)),
          () -> assertFalse(vec.getShort(3, new ShortBox()).validity)
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideCharVecs")
  public void getSetChars(LongFunction<? extends U16Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertEquals('\0', vec.getChar(0));
      assertEquals('\0', vec.getChar(3));
      vec.setChar(0, 'A');
      vec.setChar(3, 'z');
      assertEquals('A', vec.getChar(0));
      assertEquals('z', vec.getChar(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideCharVecs")
  public void getBoxChars(LongFunction<? extends U16Vec> factory) {
    try(var base = factory.apply(5);
        var vec = base.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setChar(1, 'A');
      vec.setNull(2);
      vec.setChar(3, 'z');

      var box = new CharBox();
      vec.getChar(0, box);
      assertFalse(box.validity);
      vec.getChar(1, box);
      assertTrue(box.validity);
      assertEquals('A', box.value);
      vec.getChar(2, box);
      assertFalse(box.validity);
      vec.getChar(3, box);
      assertTrue(box.validity);
      assertEquals('z', box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideCharVecs")
  public void wrapOutOfBoundsChars(LongFunction<? extends U16Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getChar(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getChar(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setChar(7, 'A')),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setChar(-1, 'A'))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideCharVecs")
  public void validityChars(LongFunction<? extends U16Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var vec = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setChar(0, 'A');
      vec.setChar(3, 'z');
      vec.setNull(0);
      vec.setNull(3);
      assertAll(
          () -> assertTrue(vec.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> vec.getChar(0)),
          () -> assertFalse(vec.getChar(0, new CharBox()).validity),

          () -> assertTrue(vec.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.getChar(3)),
          () -> assertFalse(vec.getChar(3, new CharBox()).validity)
      );
    }
  }

  @Test
  public void mapNew() throws IOException {
    var path = Files.createTempFile("map-new", "");
    Closeable andClean = () -> Files.delete(path);
    try(andClean) {
      try(var vec = U16Vec.mapNew(null, path, 128)) {
        assertEquals(128, vec.length());
      }
    }
  }

  @Test
  public void demo() throws IOException {
    var dir = Files.createTempDirectory("vec-u16");
    Closeable andClean = () -> {
      try(var stream = list(dir)) {
        for(var path: stream.toList()) {
          Files.delete(path);
        }
      }
      Files.delete(dir);
    };
    try(andClean) {
      var dataPath = dir.resolve("element");
      var validityPath = dir.resolve("validity");

      U16Vec vec;
      try (var validityBuilder = U1Vec.builder(null, validityPath);
           var builder = U16Vec.builder(validityBuilder, dataPath)) {
        LongStream.range(0, 100_000).forEach(i -> builder.appendShort((short) i));
        vec = builder.toVec();
      }
      try (vec) {
        assertEquals(100_000, vec.length());
        assertEquals((short) 6, vec.getShort(6));
        assertEquals((short) 13_658, vec.getShort(13_658));
        vec.setNull(13);
        assertTrue(vec.isNull(13));
      }
    }
  }
}