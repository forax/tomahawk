package com.github.forax.tomahawk.vec;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class VecStructBuilderTest {
  @Test
  public void builder() throws IOException {
    var pathNameData = createTempFile("struct-vec-name-element--", ".dtst");
    var pathNameOffset = createTempFile("struct-vec-name-offset--", ".dtst");
    var pathNameValidity = createTempFile("struct-vec-name-validity--", ".dtst");
    var pathAge = createTempFile("struct-vec-age--", ".dtst");
    var pathAgeValidity = createTempFile("struct-vec-age-validity--", ".dtst");
    var pathValidity = createTempFile("struct-vec-validity--", ".dtst");
    try {
      StructVec vec;
      try (var nameValidityB = U1Vec.builder(null, pathNameValidity, CREATE);
           var nameOffsetB = U32Vec.builder(null, pathNameOffset, CREATE);
           var nameDataB = U16Vec.builder(null, pathNameData, CREATE);
           var nameB = ListVec.builder(nameValidityB, nameOffsetB, nameDataB);
           var ageValidityB = U1Vec.builder(null, pathAgeValidity, CREATE);
           var ageB = U32Vec.builder(ageValidityB, pathAge, CREATE);
           var validityB = U1Vec.builder(null, pathValidity, CREATE);
           var builder = StructVec.builder(validityB, nameB, ageB)) {
        builder
            .appendRow(row -> row.appendString(nameB, "Bob").appendInt(ageB, 42))
            .appendNull()
            .appendRow(row -> row.appendValues(nameB, b -> b.appendString("Ana")));
        vec = builder.toVec();
      }
      try(vec) {
        var name = vec.fields().get(0).asListOf(U16Vec.class);
        var age = vec.fields().get(1).as(U32Vec.class);
        assertAll(
            () -> assertEquals(3, vec.length()),
            () -> assertEquals("Bob", name.getString(0)),
            () -> assertEquals(42, age.getInt(0)),
            () -> assertTrue(vec.isNull(1)),
            () -> assertEquals("Ana", name.getString(2)),
            () -> assertTrue(age.isNull(2))
        );
      }
    } finally {
      Files.deleteIfExists(pathValidity);
      Files.deleteIfExists(pathAgeValidity);
      Files.deleteIfExists(pathAge);
      Files.deleteIfExists(pathNameValidity);
      Files.deleteIfExists(pathNameOffset);
      Files.deleteIfExists(pathNameData);
    }
  }

  @Test
  public void allTypes() throws IOException {
    var pathBoolean = createTempFile("struct-vec-boolean--", ".dtst");
    var pathByte = createTempFile("struct-vec-byte--", ".dtst");
    var pathShort = createTempFile("struct-vec-short--", ".dtst");
    var pathChar = createTempFile("struct-vec-char--", ".dtst");
    var pathInt = createTempFile("struct-vec-int--", ".dtst");
    var pathFloat = createTempFile("struct-vec-float--", ".dtst");
    var pathLong = createTempFile("struct-vec-long--", ".dtst");
    var pathDouble = createTempFile("struct-vec-double--", ".dtst");
    try {
      StructVec vec;
      try (var booleanBuilder = U1Vec.builder(null, pathBoolean);
           var byteBuilder = U8Vec.builder(null, pathByte);
           var shortBuilder = U16Vec.builder(null, pathShort);
           var charBuilder = U16Vec.builder(null, pathChar);
           var intBuilder = U32Vec.builder(null, pathInt);
           var floatBuilder = U32Vec.builder(null, pathFloat);
           var longBuilder = U64Vec.builder(null, pathLong);
           var doubleBuilder = U64Vec.builder(null, pathDouble);
           var builder = StructVec.builder(null,
               booleanBuilder, byteBuilder, shortBuilder, charBuilder, intBuilder, floatBuilder, longBuilder, doubleBuilder)) {
        builder
            .appendRow(row ->
                row.appendBoolean(booleanBuilder, true)
                    .appendByte(byteBuilder, (byte) 5)
                    .appendShort(shortBuilder, (short) -7)
                    .appendChar(charBuilder, 'Z')
                    .appendInt(intBuilder, 6847)
                    .appendFloat(floatBuilder, 67.2f)
                    .appendLong(longBuilder, 78L)
                    .appendDouble(doubleBuilder, 67.8)
            )
            .appendRow(row ->
                row.appendBoolean(booleanBuilder, false)
                    .appendByte(byteBuilder, (byte) 15)
                    .appendShort(shortBuilder, (short) -70)
                    .appendChar(charBuilder, 'G')
                    .appendInt(intBuilder, 433488)
                    .appendFloat(floatBuilder, 7.2f)
                    .appendLong(longBuilder, 787L)
                    .appendDouble(doubleBuilder, 678.7)
            );
        vec = builder.toVec();
      }
      try(vec) {
        assertAll(
            () -> assertEquals(2, vec.length()),
            () -> assertTrue(((U1Vec) vec.fields().get(0)).getBoolean(0)),
            () -> assertFalse(((U1Vec) vec.fields().get(0)).getBoolean(1)),
            () -> assertEquals((byte) 5, ((U8Vec) vec.fields().get(1)).getByte(0)),
            () -> assertEquals((byte) 15, ((U8Vec) vec.fields().get(1)).getByte(1)),
            () -> assertEquals((short) -7, ((U16Vec) vec.fields().get(2)).getShort(0)),
            () -> assertEquals((short) -70, ((U16Vec) vec.fields().get(2)).getShort(1)),
            () -> assertEquals('Z', ((U16Vec) vec.fields().get(3)).getChar(0)),
            () -> assertEquals('G', ((U16Vec) vec.fields().get(3)).getChar(1)),
            () -> assertEquals(6847, ((U32Vec) vec.fields().get(4)).getInt(0)),
            () -> assertEquals(433488, ((U32Vec) vec.fields().get(4)).getInt(1)),
            () -> assertEquals(67.2f, ((U32Vec) vec.fields().get(5)).getFloat(0)),
            () -> assertEquals(7.2f, ((U32Vec) vec.fields().get(5)).getFloat(1)),
            () -> assertEquals(78L, ((U64Vec) vec.fields().get(6)).getLong(0)),
            () -> assertEquals(787L, ((U64Vec) vec.fields().get(6)).getLong(1)),
            () -> assertEquals(67.8, ((U64Vec) vec.fields().get(7)).getDouble(0)),
            () -> assertEquals(678.7, ((U64Vec) vec.fields().get(7)).getDouble(1))
        );
      }
    } finally {
      Files.deleteIfExists(pathBoolean);
      Files.deleteIfExists(pathByte);
      Files.deleteIfExists(pathShort);
      Files.deleteIfExists(pathChar);
      Files.deleteIfExists(pathInt);
      Files.deleteIfExists(pathFloat);
      Files.deleteIfExists(pathLong);
      Files.deleteIfExists(pathDouble);
    }
  }
}
