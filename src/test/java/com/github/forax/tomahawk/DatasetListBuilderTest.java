package com.github.forax.tomahawk;

import com.github.forax.tomahawk.Tomahawk.ListDataset;
import com.github.forax.tomahawk.Tomahawk.U16Dataset;
import com.github.forax.tomahawk.Tomahawk.U1Dataset;
import com.github.forax.tomahawk.Tomahawk.U32Dataset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.IntStream;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatasetListBuilderTest {
  @Test
  public void builder() throws IOException {
    var pathData = createTempFile("utf8dataset--builder--", ".dtst");
    var pathOffset = createTempFile("utf8dataset-offsetSegment--builder--", ".dtst");
    var pathValidity = createTempFile("utf8dataset-validitySegment--builder--", ".dtst");
    try {
      ListDataset<U16Dataset> dataset;
      try (var validity = U1Dataset.builder(null, pathValidity, CREATE);
           var offset = U32Dataset.builder(null, pathOffset, CREATE);
           var data = U16Dataset.builder(null, pathData, CREATE);
           var builder = ListDataset.builder(data, offset, validity)) {
        builder
            .appendValues(b -> b.appendString("foo"))
            .appendValues(b -> b.appendString(""))
            .appendValues(b -> b.appendString("bar"));
        dataset = builder.toDataset();
      }

      assertAll(
          () -> assertEquals(3, dataset.length()),
          () -> assertEquals("foo", dataset.getString(0)),
          () -> assertEquals("", dataset.getString(1)),
          () -> assertEquals("bar", dataset.getString(2))
      );
    } finally {
      Files.deleteIfExists(pathValidity);
      Files.deleteIfExists(pathOffset);
      Files.deleteIfExists(pathData);
    }
  }

  /*@Test
  public void builderNullableBoxed() throws IOException {
    var pathMask = Files.createTempFile("utf8dataset-mask--builder--", ".dtst");
    var pathArray = Files.createTempFile("utf8dataset-dataSegment--builder--", ".dtst");
    try {
      IntDataset dataset;
      try (var mask = BooleanDataset.builder(null, pathMask, CREATE);
           var builder = IntDataset.builder(mask, pathArray, CREATE)) {
        builder
            .append(-99)
            .appendNull()
            .append(777)
            .appendNull();
        dataset = builder.toDataset();
      }
      assertAll(
          () -> assertEquals(4, dataset.length()),

          () -> assertFalse(dataset.isNull(0)),
          () -> assertEquals(-99, dataset.get(0)),
          () -> assertEquals(-99, dataset.getBoxed(0)),

          () -> assertTrue(dataset.isNull(1)),
          () -> assertNull(dataset.getBoxed(1)),
          () -> assertThrows(NullPointerException.class, () -> dataset.get(1)),

          () -> assertFalse(dataset.isNull(2)),
          () -> assertEquals(777, dataset.get(2)),
          () -> assertEquals(777, dataset.getBoxed(2)),

          () -> assertTrue(dataset.isNull(3)),
          () -> assertNull(dataset.getBoxed(3)),
          () -> assertThrows(NullPointerException.class, () -> dataset.get(3))
      );
    } finally {
      Files.deleteIfExists(pathArray);
      Files.deleteIfExists(pathMask);
    }
  }*/

  @Test
  public void builderALot() throws IOException {
    var offsetPath = createTempFile("listdataset--builderALot--", ".dtst");
    var dataPath = createTempFile("listdataset--builderALot--", ".dtst");
    try {
      ListDataset<U16Dataset> dataset;
      try (var offsetBuilder = U32Dataset.builder(null, offsetPath, CREATE);
           var dataBuilder = U16Dataset.builder(null, dataPath, CREATE);
           var builder = ListDataset.builder(dataBuilder, offsetBuilder, null)) {
        IntStream.range(0, 10_000_000)
            .mapToObj(i -> "" + i)
            .forEach(s -> builder.appendValues(b -> b.appendString(s)));
        dataset = builder.toDataset();
      }
      IntStream.range(0, 10_000_000).forEach(i -> assertEquals("" + i, dataset.getString(i)));
      dataset.close();
    } finally {
      Files.delete(dataPath);
      Files.delete(offsetPath);
    }
  }
}