package com.github.forax.tomahawk;

import com.github.forax.tomahawk.Tomahawk.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetU32BuilderTest {
  @Test
  public void builderWithInts() throws IOException {
    var path = createTempFile("u32dataset--builder--", ".dtst");
    try {
      U32Dataset dataset;
      try (var builder = U32Dataset.builder(null, path, CREATE)) {
        builder
            .appendInt(123)
            .appendInt(42)
            .appendInt(-5);
        dataset = builder.toDataset();
      }
      assertAll(
          () -> assertEquals(3, dataset.length()),
          () -> assertEquals(123, dataset.getInt(0)),
          () -> assertEquals(42, dataset.getInt(1)),
          () -> assertEquals(-5, dataset.getInt(2))
      );
    } finally {
      Files.deleteIfExists(path);
    }
  }

  @Test
  public void builderWithNullableInts() throws IOException {
    var pathValidity = createTempFile("u32dataset-validitySegment--builder--", ".dtst");
    var pathData = createTempFile("u32dataset-dataSegment--builder--", ".dtst");
    try {
      U32Dataset dataset;
      try (var validity = U1Dataset.builder(null, pathValidity, CREATE);
           var builder = U32Dataset.builder(validity, pathData, CREATE)) {
        builder
            .appendInt(-99)
            .appendNull()
            .appendInt(777)
            .appendNull();
        dataset = builder.toDataset();
      }
      assertAll(
          () -> assertEquals(4, dataset.length()),

          () -> assertFalse(dataset.isNull(0)),
          () -> assertEquals(-99, dataset.getInt(0)),
          () -> dataset.getInt(0, (validity, value) -> {
            assertTrue(validity);
            assertEquals(-99, value);
          }),

          () -> assertTrue(dataset.isNull(1)),
          () -> dataset.getInt(1, (validity, __) -> assertFalse(validity)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getInt(1)),

          () -> assertFalse(dataset.isNull(2)),
          () -> assertEquals(777, dataset.getInt(2)),
          () -> dataset.getInt(2, (validity, value) -> {
            assertTrue(validity);
            assertEquals(777, value);
          }),

          () -> assertTrue(dataset.isNull(3)),
          () -> dataset.getInt(3, (validity, __) -> assertFalse(validity)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getInt(3))
      );
    } finally {
      Files.deleteIfExists(pathData);
      Files.deleteIfExists(pathValidity);
    }
  }

  @Test
  public void builderWithFloats() throws IOException {
    var path = createTempFile("u32dataset--builder--", ".dtst");
    try {
      U32Dataset dataset;
      try (var builder = U32Dataset.builder(null, path, CREATE)) {
        builder
            .appendFloat(12.0f)
            .appendFloat(42)
            .appendFloat(-8);
        dataset = builder.toDataset();
      }
      assertAll(
          () -> assertEquals(3, dataset.length()),
          () -> assertEquals(12.0f, dataset.getFloat(0)),
          () -> assertEquals(42.0f, dataset.getFloat(1)),
          () -> assertEquals(-8.0f, dataset.getFloat(2))
      );
    } finally {
      Files.deleteIfExists(path);
    }
  }

  @Test
  public void builderWithNullableFloats() throws IOException {
    var pathValidity = createTempFile("u32dataset-validitySegment--builder--", ".dtst");
    var pathData = createTempFile("u32dataset-dataSegment--builder--", ".dtst");
    try {
      U32Dataset dataset;
      try (var validity = U1Dataset.builder(null, pathValidity, CREATE);
           var builder = U32Dataset.builder(validity, pathData, CREATE)) {
        builder
            .appendFloat(-888.0f)
            .appendNull()
            .appendFloat(44.0f)
            .appendNull();
        dataset = builder.toDataset();
      }
      assertAll(
          () -> assertEquals(4, dataset.length()),

          () -> assertFalse(dataset.isNull(0)),
          () -> assertEquals(-888.0f, dataset.getFloat(0)),
          () -> dataset.getFloat(0, (validity, value) -> {
            assertTrue(validity);
            assertEquals(-888.0f, value);
          }),

          () -> assertTrue(dataset.isNull(1)),
          () -> dataset.getFloat(1, (validity, __) -> assertFalse(validity)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getFloat(1)),

          () -> assertFalse(dataset.isNull(2)),
          () -> assertEquals(44.0f, dataset.getFloat(2)),
          () -> dataset.getFloat(2, (validity, value) -> {
            assertTrue(validity);
            assertEquals(44.0f, value);
          }),

          () -> assertTrue(dataset.isNull(3)),
          () -> dataset.getInt(3, (validity, __) -> assertFalse(validity)),
          () -> assertThrows(NullPointerException.class, () -> dataset.getInt(3))
      );
    } finally {
      Files.deleteIfExists(pathData);
      Files.deleteIfExists(pathValidity);
    }
  }

  @Test
  public void builderALot() throws IOException {
    var path = createTempFile("u32dataset--builderALot--", ".dtst");
    U32Dataset dataset;
    try(var builder = U32Dataset.builder(null, path, CREATE)) {
      range(0, 100_000_000)
          .forEach(builder::appendInt);
      dataset = builder.toDataset();
    }
    range(0, 100_000_000).forEach(i -> assertEquals(i, dataset.getInt(i)));
    dataset.close();
    Files.delete(path);
  }
}