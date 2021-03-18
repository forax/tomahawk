package com.github.forax.tomahawk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("static-method")
public class VecStructBuilderTest {
  @Test
  public void builder() throws IOException {
    var pathNameData = createTempFile("struct-dataset-name-data--", ".dtst");
    var pathNameOffset = createTempFile("struct-dataset-name-offset--", ".dtst");
    var pathNameValidity = createTempFile("struct-dataset-name-validity--", ".dtst");
    var pathAge = createTempFile("struct-dataset-age--", ".dtst");
    var pathAgeValidity = createTempFile("struct-dataset-age-validity--", ".dtst");
    var pathValidity = createTempFile("struct-dataset-validity--", ".dtst");
    try {
      StructVec dataset;
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
        dataset = builder.toVec();
      }

      assertAll(
          () -> assertEquals(3, dataset.length()),
          () -> assertEquals("Bob", name.getString(0)),
          () -> assertEquals(42, age.getInt(0)),
          () -> assertTrue(dataset.isNull(1)),
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
