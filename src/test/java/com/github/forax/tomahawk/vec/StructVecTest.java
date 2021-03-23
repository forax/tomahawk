package com.github.forax.tomahawk.vec;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.IntStream;

import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructVecTest {
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
      var offsetNamePath = dir.resolve("offset-name");
      var namePath = dir.resolve("name");
      var salaryPath = dir.resolve("salary");
      var validityPath = dir.resolve("validity");

      StructVec vec;
      try(var validityBuilder = U1Vec.builder(null, validityPath);
          var name = ListVec.builder(null,                              // a list of list of U16
              U32Vec.builder(null, offsetNamePath),
              U16Vec.builder(null, namePath));
          var salary = U32Vec.builder(null, salaryPath);                // a list of U32
          var builder = StructVec.builder(validityBuilder, name, salary)) {
        IntStream.range(0, 100_000).forEach(i -> {
          builder.appendRow(rowBuilder -> {
            rowBuilder.appendString(name, "person" + i)
                .appendInt(salary, 1_000 + i % 100);
          });
        });
        vec = builder.toVec();
      }
      try (vec) {
        assertEquals(100_000, vec.length());

        var name = (ListVec<?>) vec.fields().get(0);
        var salary = (U32Vec) vec.fields().get(1);

        assertEquals("person757", name.getString(757));
        assertEquals(TextWrap.from("person757"), name.getTextWrap(757));
        assertEquals("person757", name.getTextWrap(757).toString());
        assertEquals(1_057, salary.getInt(757));

        vec.setNull(13);
        assertTrue(vec.isNull(13));
      }
    }
  }
}
