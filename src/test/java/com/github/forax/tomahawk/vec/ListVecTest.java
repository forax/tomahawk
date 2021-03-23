package com.github.forax.tomahawk.vec;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.LongStream;

import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListVecTest {
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
      var dataPath = dir.resolve("data");
      var offsetPath = dir.resolve("offset");
      var validityPath = dir.resolve("validity");

      ListVec<U8Vec> vec;   // each value is a list of U8
      try(var validityBuilder = U1Vec.builder(null, validityPath);
          var offsetBuilder = U32Vec.builder(null, offsetPath);
          var dataBuilder = U8Vec.builder(null, dataPath);
          var builder = ListVec.builder(validityBuilder, offsetBuilder, dataBuilder)) {
        LongStream.range(0, 100_000).forEach(i -> {
          builder.appendValues(b -> {   // append the list of values
            b.appendByte((byte) (i % 10))
             .appendByte((byte) -5);
          });
        });
        vec = builder.toVec();
      }
      try (vec) {
        assertEquals(100_000, vec.length());

        var data = vec.data();
        var box = new ValuesBox();
        vec.getValues(6, box);   // extract the validity, startOffset and endOffset
        assertEquals(6, data.getByte(box.startOffset));        // first item
        assertEquals(-5, data.getByte(box.startOffset + 1));   // second item

        vec.setNull(13);
        assertTrue(vec.isNull(13));
      }
    }
  }

  @Test
  public void textWrap() throws IOException {
    var dir = Files.createTempDirectory("text-wrap");
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
      var offsetPath = dir.resolve("offset");
      var validityPath = dir.resolve("validity");

      ListVec<U16Vec> vec;   // each value is a list of U16 aka a string
      try(var validityBuilder = U1Vec.builder(null, validityPath);
          var offsetBuilder = U32Vec.builder(null, offsetPath);
          var dataBuilder = U16Vec.builder(null, dataPath);
          var builder = ListVec.builder(validityBuilder, offsetBuilder, dataBuilder)) {
        LongStream.range(0, 100_000)
            .forEach(i -> builder.appendString("" + i));
        vec = builder.toVec();
      }
      try (vec) {
        assertEquals(100_000, vec.length());

        // more efficient
        assertTrue(vec.textWraps().anyMatch(TextWrap.from("42")::equals));

        // less efficient
        assertTrue(vec.textWraps().map(TextWrap::toString).anyMatch("42"::equals));
      }
    }
  }
}
