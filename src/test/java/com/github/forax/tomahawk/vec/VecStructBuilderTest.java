package com.github.forax.tomahawk.vec;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class VecStructBuilderTest {
  @Test
  public void builder() throws IOException {
    var pathNameData = createTempFile("struct-vec-name-data--", ".dtst");
    var pathNameOffset = createTempFile("struct-vec-name-offset--", ".dtst");
    var pathNameValidity = createTempFile("struct-vec-name-validity--", ".dtst");
    var pathAge = createTempFile("struct-vec-age--", ".dtst");
    var pathAgeValidity = createTempFile("struct-vec-age-validity--", ".dtst");
    var pathValidity = createTempFile("struct-vec-validity--", ".dtst");
    try {
      StructVec vec;
      ListVec<U16Vec> name;
      U32Vec age;
      try (var nameValidityB = U1Vec.builder(null, pathNameValidity, CREATE);
           var nameOffsetB = U32Vec.builder(null, pathNameOffset, CREATE);
           var nameDataB = U16Vec.builder(null, pathNameData, CREATE);
           var nameB = ListVec.builder(nameDataB, nameOffsetB, nameValidityB);
           var ageValidityB = U1Vec.builder(null, pathAgeValidity, CREATE);
           var ageB = U32Vec.builder(ageValidityB, pathAge, CREATE);
           var validityB = U1Vec.builder(null, pathValidity, CREATE);
           var builder = StructVec.builder(validityB, nameB, ageB)) {
        builder
            .appendRow(row -> row.appendString(nameB, "Bob").appendInt(ageB, 42))
            .appendNull()
            .appendRow(row -> row.appendValues(nameB, b -> b.appendString("Ana")));
        name = nameB.toVec();
        age = ageB.toVec();
        vec = builder.toVec();
      }

      assertAll(
          () -> assertEquals(3, vec.length()),
          () -> assertEquals("Bob", name.getString(0)),
          () -> assertEquals(42, age.getInt(0)),
          () -> assertTrue(vec.isNull(1)),
          () -> assertEquals("Ana", name.getString(2)),
          () -> assertTrue(age.isNull(2))
      );
    } finally {
      Files.deleteIfExists(pathValidity);
      Files.deleteIfExists(pathAgeValidity);
      Files.deleteIfExists(pathAge);
      Files.deleteIfExists(pathNameValidity);
      Files.deleteIfExists(pathNameOffset);
      Files.deleteIfExists(pathNameData);
    }
  }
}
