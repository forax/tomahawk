package com.github.forax.tomahawk.vec;

import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class VecU1Test {
  @Test
  public void demo() throws IOException {
    var dir = Files.createTempDirectory("vec-u1");
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

      U1Vec vec;
      try (var validityBuilder = U1Vec.builder(null, validityPath);
           var builder = U1Vec.builder(validityBuilder, dataPath)) {
        LongStream.range(0, 100_000).forEach(i -> builder.appendBoolean(i % 2 == 0));
        vec = builder.toVec();
      }
      try (vec) {
        assertEquals(100_032, vec.length());   // values are aligned to 64 bits
        assertTrue(vec.getBoolean(6));
        assertFalse(vec.getBoolean(777));
        vec.setNull(13);
        assertTrue(vec.isNull(13));
      }
    }
  }
}