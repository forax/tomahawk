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
public class VecU64Test {
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U64Vec>> provideLongVecs() {
    return Stream.of(
        length -> U64Vec.wrap(new long[(int) length]),
        length -> U64Vec.from(null, MemorySegment.allocateNative(length * 8))
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U64Vec>> provideDoubleVecs() {
    return Stream.of(
        length -> U64Vec.wrap(new double[(int) length]),
        length -> U64Vec.from(null, MemorySegment.allocateNative(length * 8))
    );
  }
  @SuppressWarnings("unused")
  public static Stream<LongFunction<U64Vec>> provideAllVecs() {
    return Stream.of(
        length -> U64Vec.wrap(new long[(int) length]),
        length -> U64Vec.wrap(new double[(int) length]),
        length -> U64Vec.from(null, MemorySegment.allocateNative(length * 8))
    );
  }

  @ParameterizedTest
  @MethodSource("provideAllVecs")
  public void length(LongFunction<? extends U64Vec> factory) {
    assertAll(
        () -> {
          try(var vec = factory.apply(13)) {
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
  public void notNullableByDefault(LongFunction<? extends U64Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertFalse(vec.isNull(3)),
          () -> assertThrows(IllegalStateException.class, () -> vec.setNull(3))
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideLongVecs")
  public void getSetLongs(LongFunction<? extends U64Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertEquals(0, vec.getLong(0));
      assertEquals(0, vec.getLong(3));
      vec.setLong(0, 42L);
      vec.setLong(3, 56L);
      assertEquals(42L, vec.getLong(0));
      assertEquals(56L, vec.getLong(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideLongVecs")
  public void getBoxLongs(LongFunction<? extends U64Vec> factory) {
    try(var base = factory.apply(5);
        var vec = base.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setLong(1, 1324L);
      vec.setNull(2);
      vec.setLong(3, 2768);

      var box = new LongBox();
      vec.getLong(0, box);
      assertFalse(box.validity);
      vec.getLong(1, box);
      assertTrue(box.validity);
      assertEquals(1324L, box.value);
      vec.getLong(2, box);
      assertFalse(box.validity);
      vec.getLong(3, box);
      assertTrue(box.validity);
      assertEquals(2768L, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideLongVecs")
  public void wrapOutOfBoundsLongs(LongFunction<? extends U64Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getLong(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getLong(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setLong(7, 42L)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setLong(-1, 42L))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideLongVecs")
  public void validityLongs(LongFunction<? extends U64Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var vec = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setLong(0, 42L);
      vec.setLong(3, 56L);
      vec.setNull(0);
      vec.setNull(3);
      assertAll(
          () -> assertTrue(vec.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> vec.getLong(0)),
          () -> assertFalse(vec.getLong(0, new LongBox()).validity),

          () -> assertTrue(vec.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.getLong(3)),
          () -> assertFalse(vec.getLong(3, new LongBox()).validity)
      );
    }
  }


  @ParameterizedTest
  @MethodSource("provideDoubleVecs")
  public void getSetDoubles(LongFunction<? extends U64Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertEquals(0.0, vec.getDouble(0));
      assertEquals(0.0, vec.getDouble(3));
      vec.setDouble(0, 42.0);
      vec.setDouble(3, 56.0);
      assertEquals(42.0, vec.getDouble(0));
      assertEquals(56.0, vec.getDouble(3));
    }
  }

  @ParameterizedTest
  @MethodSource("provideDoubleVecs")
  public void getBoxDoubles(LongFunction<? extends U64Vec> factory) {
    try(var base = factory.apply(5);
        var vec = base.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setDouble(1, 1324.0);
      vec.setNull(2);
      vec.setDouble(3, 2768.0);

      var box = new DoubleBox();
      vec.getDouble(0, box);
      assertFalse(box.validity);
      vec.getDouble(1, box);
      assertTrue(box.validity);
      assertEquals(1324.0, box.value);
      vec.getDouble(2, box);
      assertFalse(box.validity);
      vec.getDouble(3, box);
      assertTrue(box.validity);
      assertEquals(2768.0, box.value);
    }
  }

  @ParameterizedTest
  @MethodSource("provideDoubleVecs")
  public void wrapOutOfBoundsDoubles(LongFunction<? extends U64Vec> factory) {
    try(var vec = factory.apply(5)) {
      assertAll(
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getDouble(7)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.getDouble(-1)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setDouble(7, 42.0)),
          () -> assertThrows(IndexOutOfBoundsException.class, () -> vec.setDouble(-1, 42.0))
      );
    }
  }

  @ParameterizedTest
  @MethodSource("provideDoubleVecs")
  public void validityDoubles(LongFunction<? extends U64Vec> factory) {
    try(var simpleDataset = factory.apply(5);
        var vec = simpleDataset.withValidity(U1Vec.wrap(new long[1]))) {
      vec.setDouble(0, 42.0);
      vec.setDouble(3, 56.0);
      vec.setNull(0);
      vec.setNull(3);
      assertAll(
          () -> assertTrue(vec.isNull(0)),
          () -> assertThrows(NullPointerException.class, () -> vec.getDouble(0)),
          () -> assertFalse(vec.getDouble(0, new DoubleBox()).validity),

          () -> assertTrue(vec.isNull(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.getDouble(3)),
          () -> assertFalse(vec.getDouble(3, new DoubleBox()).validity)
      );
    }
  }

  @Test
  public void mapNew() throws IOException {
    var path = Files.createTempFile("map-new", "");
    Closeable andClean = () -> Files.delete(path);
    try(andClean) {
      try(var vec = U64Vec.mapNew(null, path, 128)) {
        assertEquals(128, vec.length());
      }
    }
  }

  @Test
  public void demo() throws IOException {
    var dir = Files.createTempDirectory("vec-u32");
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

      U64Vec vec;
      try (var validityBuilder = U1Vec.builder(null, validityPath);
           var builder = U64Vec.builder(validityBuilder, dataPath)) {
        LongStream.range(0, 100_000).forEach(builder::appendLong);
        vec = builder.toVec();
      }
      try (vec) {
        assertEquals(100_000, vec.length());
        assertEquals(6, vec.getLong(6));
        assertEquals(78_453, vec.getLong(78_453));
        vec.setNull(13);
        assertTrue(vec.isNull(13));
      }
    }
  }
}