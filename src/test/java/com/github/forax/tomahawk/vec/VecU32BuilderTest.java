package com.github.forax.tomahawk.vec;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class VecU32BuilderTest {
  @Test
  public void builderWithInts() throws IOException {
    var path = createTempFile("u32-vec--builder--", ".dtst");
    try {
      U32Vec vec;
      try (var builder = U32Vec.builder(null, path, CREATE)) {
        builder
            .appendInt(123)
            .appendInt(42)
            .appendInt(-5);
        vec = builder.toVec();
      }
      try(vec) {
        assertAll(
            () -> assertEquals(3, vec.length()),
            () -> assertEquals(123, vec.getInt(0)),
            () -> assertEquals(42, vec.getInt(1)),
            () -> assertEquals(-5, vec.getInt(2))
        );
      }
    } finally {
      Files.deleteIfExists(path);
    }
  }

  @Test
  public void builderWithNullableInts() throws IOException {
    var pathValidity = createTempFile("u32-vec-validity--builder--", ".dtst");
    var pathData = createTempFile("u32-vec-element--builder--", ".dtst");
    try {
      U32Vec vec;
      try (var validity = U1Vec.builder(null, pathValidity, CREATE);
           var builder = U32Vec.builder(validity, pathData, CREATE)) {
        builder
            .appendInt(-99)
            .appendNull()
            .appendInt(777)
            .appendNull();
        vec = builder.toVec();
      }
      try(vec) {
        assertAll(
            () -> assertEquals(4, vec.length()),

            () -> assertFalse(vec.isNull(0)),
            () -> assertEquals(-99, vec.getInt(0)),
            () -> assertTrue(vec.getInt(0, new IntBox()).validity),
            () -> assertEquals(-99, vec.getInt(0, new IntBox()).value),

            () -> assertTrue(vec.isNull(1)),
            () -> assertFalse(vec.getInt(1, new IntBox()).validity),
            () -> assertThrows(NullPointerException.class, () -> vec.getInt(1)),

            () -> assertFalse(vec.isNull(2)),
            () -> assertEquals(777, vec.getInt(2)),
            () -> assertTrue(vec.getInt(2, new IntBox()).validity),
            () -> assertEquals(777, vec.getInt(2, new IntBox()).value),

            () -> assertTrue(vec.isNull(3)),
            () -> assertFalse(vec.getInt(3, new IntBox()).validity),
            () -> assertThrows(NullPointerException.class, () -> vec.getInt(3))
        );
      }
    } finally {
      Files.deleteIfExists(pathData);
      Files.deleteIfExists(pathValidity);
    }
  }

  @Test
  public void builderWithFloats() throws IOException {
    var path = createTempFile("u32-vec--builder--", ".dtst");
    try {
      U32Vec vec;
      try (var builder = U32Vec.builder(null, path, CREATE)) {
        builder
            .appendFloat(12.0f)
            .appendFloat(42)
            .appendFloat(-8);
        vec = builder.toVec();
      }
      try(vec) {
        assertAll(
            () -> assertEquals(3, vec.length()),
            () -> assertEquals(12.0f, vec.getFloat(0)),
            () -> assertEquals(42.0f, vec.getFloat(1)),
            () -> assertEquals(-8.0f, vec.getFloat(2))
        );
      }
    } finally {
      Files.deleteIfExists(path);
    }
  }

  @Test
  public void builderWithNullableFloats() throws IOException {
    var pathValidity = createTempFile("u32-vec-validity--builder--", ".dtst");
    var pathData = createTempFile("u32-vec-element--builder--", ".dtst");
    try {
      U32Vec vec;
      try (var validity = U1Vec.builder(null, pathValidity, CREATE);
           var builder = U32Vec.builder(validity, pathData, CREATE)) {
        builder
            .appendFloat(-888.0f)
            .appendNull()
            .appendFloat(44.0f)
            .appendNull();
        vec = builder.toVec();
      }
      try(vec) {
        assertAll(
            () -> assertEquals(4, vec.length()),

            () -> assertFalse(vec.isNull(0)),
            () -> assertEquals(-888.0f, vec.getFloat(0)),
            () -> assertTrue(vec.getFloat(0, new FloatBox()).validity),
            () -> assertEquals(-888.0f, vec.getFloat(0, new FloatBox()).value),

            () -> assertTrue(vec.isNull(1)),
            () -> assertFalse(vec.getFloat(1, new FloatBox()).validity),
            () -> assertThrows(NullPointerException.class, () -> vec.getFloat(1)),

            () -> assertFalse(vec.isNull(2)),
            () -> assertEquals(44.0f, vec.getFloat(2)),
            () -> assertTrue(vec.getFloat(2, new FloatBox()).validity),
            () -> assertEquals(44.0f, vec.getFloat(2, new FloatBox()).value),

            () -> assertTrue(vec.isNull(3)),
            () -> assertFalse(vec.getInt(3, new IntBox()).validity),
            () -> assertThrows(NullPointerException.class, () -> vec.getInt(3))
        );
      }
    } finally {
      Files.deleteIfExists(pathData);
      Files.deleteIfExists(pathValidity);
    }
  }

  @Test
  public void builderALot() throws IOException {
    var path = createTempFile("u32-vec--builder-a-lot--", ".dtst");
    U32Vec vec;
    try(var builder = U32Vec.builder(null, path, CREATE)) {
      range(0, 100_000_000)
          .forEach(builder::appendInt);
      vec = builder.toVec();
    }
    try(vec) {
      range(0, 100_000_000).forEach(i -> assertEquals(i, vec.getInt(i)));
    }
    Files.delete(path);
  }
}