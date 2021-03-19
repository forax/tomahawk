package com.github.forax.tomahawk.vec;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class VecListBuilderTest {
  @Test
  public void builder() throws IOException {
    var pathData = createTempFile("list-vec--builder--", ".dtst");
    var pathOffset = createTempFile("list-vec-offset--builder--", ".dtst");
    var pathValidity = createTempFile("list-vec-validity--builder--", ".dtst");
    try {
      ListVec<U16Vec> vec;
      try (var validity = U1Vec.builder(null, pathValidity, CREATE);
           var offset = U32Vec.builder(null, pathOffset, CREATE);
           var data = U16Vec.builder(null, pathData, CREATE);
           var builder = ListVec.builder(data, offset, validity)) {
        builder
            .appendValues(b -> b.appendString("foo"))
            .appendValues(b -> b.appendString(""))
            .appendValues(b -> b.appendString("bar"));
        vec = builder.toVec();
      }

      assertAll(
          () -> assertEquals(3, vec.length()),
          () -> assertEquals("foo", vec.getString(0)),
          () -> assertEquals("", vec.getString(1)),
          () -> assertEquals("bar", vec.getString(2))
      );
    } finally {
      Files.deleteIfExists(pathValidity);
      Files.deleteIfExists(pathOffset);
      Files.deleteIfExists(pathData);
    }
  }

  /*@Test
  public void builderNullableBoxed() throws IOException {
    var pathMask = Files.createTempFile("utf8vec-mask--builder--", ".dtst");
    var pathArray = Files.createTempFile("utf8vec-dataSegment--builder--", ".dtst");
    try {
      IntDataset vec;
      try (var mask = BooleanDataset.builder(null, pathMask, CREATE);
           var builder = IntDataset.builder(mask, pathArray, CREATE)) {
        builder
            .append(-99)
            .appendNull()
            .append(777)
            .appendNull();
        vec = builder.toDataset();
      }
      assertAll(
          () -> assertEquals(4, vec.length()),

          () -> assertFalse(vec.isNull(0)),
          () -> assertEquals(-99, vec.get(0)),
          () -> assertEquals(-99, vec.getBoxed(0)),

          () -> assertTrue(vec.isNull(1)),
          () -> assertNull(vec.getBoxed(1)),
          () -> assertThrows(NullPointerException.class, () -> vec.get(1)),

          () -> assertFalse(vec.isNull(2)),
          () -> assertEquals(777, vec.get(2)),
          () -> assertEquals(777, vec.getBoxed(2)),

          () -> assertTrue(vec.isNull(3)),
          () -> assertNull(vec.getBoxed(3)),
          () -> assertThrows(NullPointerException.class, () -> vec.get(3))
      );
    } finally {
      Files.deleteIfExists(pathArray);
      Files.deleteIfExists(pathMask);
    }
  }*/

  @Test
  public void builderALot() throws IOException {
    var offsetPath = createTempFile("list-vec--builderALot--", ".dtst");
    var dataPath = createTempFile("list-vec--builderALot--", ".dtst");
    try {
      ListVec<U16Vec> vec;
      try (var offsetBuilder = U32Vec.builder(null, offsetPath, CREATE);
           var dataBuilder = U16Vec.builder(null, dataPath, CREATE);
           var builder = ListVec.builder(dataBuilder, offsetBuilder, null)) {
        IntStream.range(0, 10_000_000)
            .mapToObj(i -> "" + i)
            .forEach(s -> builder.appendValues(b -> b.appendString(s)));
        vec = builder.toVec();
      }
      try(vec) {
        IntStream.range(0, 10_000_000).forEach(i -> assertEquals("" + i, vec.getString(i)));
      }
    } finally {
      Files.delete(dataPath);
      Files.delete(offsetPath);
    }
  }
}