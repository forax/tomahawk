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
import java.nio.file.Path;
import java.util.function.LongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jdk.incubator.foreign.MemorySegment;

@SuppressWarnings("static-method")
public class VecU8Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U8Vec>> provideByteVecs() {
    return Stream.of(
        length -> U8Vec.wrap(new byte[(int) length]),
        length -> U8Vec.from(null, MemorySegment.allocateNative(length))
    );
  }

  @ParameterizedTest
  @MethodSource("provideByteVecs")
  public void length(LongFunction<? extends U8Vec> factory) {
    assertAll(
        () -> assertEquals(13, factory.apply(13).length()),
        () -> assertEquals(42, factory.apply(42).length())
    );
  }

  @ParameterizedTest
  @MethodSource("provideByteVecs")
  public void notNullableByDefault(LongFunction<? extends U8Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertFalse(vec.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> vec.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideByteVecs")
  public void getSetBytes(LongFunction<? extends U8Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertEquals(0, vec.getByte(0));
      assertEquals(0, vec.getByte(3));
      vec.setByte(0, (byte) 42);
      vec.setByte(3, (byte) 56);
      assertEquals(42, vec.getByte(0));
      assertEquals(56, vec.getByte(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideByteVecs")
  public void getBoxBytes(LongFunction<? extends U8Vec> factory) {
    try(var base = factory.apply(5);
        var vec = base.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setByte(1, (byte) 124);
      vec.setNull(2);
      vec.setByte(3, (byte) -68);

      var box = new ByteBox();
      vec.getByte(0, box);
      assertFalse(box.validity);
      vec.getByte(1, box);
      assertTrue(box.validity);
      assertEquals((byte) 124, box.value);
      vec.getByte(2, box);
      assertFalse(box.validity);
      vec.getByte(3, box);
      assertTrue(box.validity);
      assertEquals((byte) -68, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideByteVecs")
  public void wrapOutOfBoundsInts(LongFunction<? extends U8Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getByte(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getByte(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setByte(7, (byte) 42)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setByte(-1, (byte) 42))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideByteVecs")
  public void validityBytes(LongFunction<? extends U8Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var vec = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setByte(0, (byte) 42);
      vec.setByte(3, (byte) 56);
      vec.setNull(0);
      vec.setNull(3);
      assertAll(
          () -> assertTrue(vec.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> vec.getByte(0)),
          () -> vec.getByte(0, (validity, __) -> assertFalse(validity)),

          () -> assertTrue(vec.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.getByte(3)),
          () -> vec.getByte(3, (validity, __) -> assertFalse(validity))
      );
    }
  }

  @Test
  public void mapNew() throws IOException {
    var path = Files.createTempFile("map-new", "");
    Closeable andClean = () -> Files.delete(path);
    try(andClean) {
      var vec = U8Vec.mapNew(null, path, 128);
      assertEquals(128, vec.length());
    }
  }

  @Test
  public void demo() throws IOException {
    var dir = Files.createTempDirectory("vec-u8");
    Closeable andClean = () -> {
      try(var stream = list(dir)) {
        for(var path: stream.toList()) {
          Files.delete(path);
        }
      }
      Files.delete(dir);
    };
    try(andClean) {
      var dataPath = dir.resolve("data");
      var validityPath = dir.resolve("validity");

      U8Vec vec;
      try (var validityBuilder = U1Vec.builder(null, validityPath);
           var builder = U8Vec.builder(validityBuilder, dataPath)) {
        LongStream.range(0, 100_000).forEach(i -> builder.appendByte((byte) i));
        vec = builder.toVec();
      }
      try (vec) {
        assertEquals(100000, vec.length());
        assertEquals((byte) 6, vec.getByte(6));
        assertEquals((byte) 120, vec.getByte(120));
        vec.setNull(13);
        assertTrue(vec.isNull(13));
      }
    }
  }
}