package com.github.forax.tomahawk.schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.byte8;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.char16;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.double64;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.float32;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.int32;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.long64;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.short16;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.u1;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

interface Layout /*permits Layout.PrimitiveLayout, Layout.ListLayout, Layout.StructLayout*/ {
  default boolean isPrimitive() {
    return this instanceof PrimitiveLayout;
  }
  default boolean isList() {
    return this instanceof ListLayout;
  }
  default boolean isStruct() {
    return this instanceof StructLayout;
  }
  boolean nullable();
  default List<Field> fields() {
    return List.of();
  }
  default Field field(String name) {
    throw new IllegalStateException("no field " + name);
  }
  default int fieldIndex(String name) {
    return -1;
  }
  default Layout element() {
    throw new IllegalStateException("no element");
  }

  static PrimitiveLayout u1(boolean nullable) {
    return new PrimitiveLayout(nullable, u1);
  }
  static PrimitiveLayout byte8(boolean nullable) {
    return new PrimitiveLayout(nullable, byte8);
  }
  static PrimitiveLayout short16(boolean nullable) {
    return new PrimitiveLayout(nullable, short16);
  }
  static PrimitiveLayout char16(boolean nullable) {
    return new PrimitiveLayout(nullable, char16);
  }
  static PrimitiveLayout int32(boolean nullable) {
    return new PrimitiveLayout(nullable, int32);
  }
  static PrimitiveLayout float32(boolean nullable) {
    return new PrimitiveLayout(nullable, float32);
  }
  static PrimitiveLayout long64(boolean nullable) {
    return new PrimitiveLayout(nullable, long64);
  }
  static PrimitiveLayout double64(boolean nullable) {
    return new PrimitiveLayout(nullable, double64);
  }

  static ListLayout list(boolean nullable, Layout layout) {
    return new ListLayout(nullable, layout);
  }
  static ListLayout string(boolean nullable) {
    return list(true, char16(nullable));
  }

  static Field field(String name, Layout layout) {
    return new Field(name, layout);
  }
  static StructLayout struct(boolean nullable, Field... fields) {
    return new StructLayout(nullable, fields);
  }

  static void save(Path path, Layout layout) throws IOException {
    Files.writeString(path, layout.toString());
  }

  static Layout load(Path path) throws IOException {
    var text = Files.readString(path);
    return parse(text);
  }

  static Layout parse(String text) {
    return LayoutParser.parse(text);
  }

  private static String toString(String space, Layout layout) {
    if (layout instanceof PrimitiveLayout primitiveLayout) {
      return primitiveLayout.toString();
    }
    if (layout instanceof ListLayout listLayout) {
      if (listLayout.element instanceof PrimitiveLayout elementLayout && elementLayout.kind == char16) {
        return "string(" + elementLayout.nullable + ")";
      }
      return "list(" + listLayout.nullable + ", " + toString(space, listLayout.element);
    }
    if (layout instanceof StructLayout structLayout) {
      if (structLayout.fieldMap.fields.isEmpty()) {
        return "struct(" + structLayout.nullable + ")";
      }
      var newSpace = space + "   ";
      return structLayout.fieldMap.fields.stream()
          .map(field -> newSpace + "field(\"" + field.name + "\", " + toString(newSpace, field.layout) + ")")
          .collect(joining(",\n", "struct(" + structLayout.nullable + ",\n", "\n" + space + ")"));
    }
    throw new AssertionError();
  }

  record PrimitiveLayout(boolean nullable, Kind kind) implements Layout {
    enum Kind {
      u1, byte8, short16, char16, int32, float32, long64, double64
    }

    public PrimitiveLayout {
      requireNonNull(kind);
    }

    @Override
    public String toString() {
      return kind + "(" + nullable + ")";
    }
  }

  record ListLayout(boolean nullable, Layout element) implements Layout {
    public ListLayout {
      requireNonNull(element);
      if (element instanceof StructLayout) {
        throw new IllegalArgumentException("a struct layout is already a list");
      }
    }

    @Override
    public String toString() {
      return Layout.toString("", this);
    }
  }

  record StructLayout(boolean nullable, FieldMap fieldMap) implements Layout {
    public StructLayout(boolean nullable, Field... fields) {
      this(nullable, new FieldMap(fields));
    }

    @Override
    public List<Field> fields() {
      //noinspection AssignmentOrReturnOfFieldWithMutableType
      return fieldMap.fields;
    }

    @Override
    public Field field(String name) {
      return fieldMap.field(name);
    }

    @Override
    public int fieldIndex(String name) {
      return fieldMap.fieldIndex(name);
    }

    @Override
    public String toString() {
      return Layout.toString("", this);
    }
  }

  final class FieldMap {
    private record FieldIndex(Field field, int index) {}

    private final List<Field> fields;
    private final HashMap<String, FieldIndex> fieldMap;

    public FieldMap(Field... fields) {
      var fieldList = List.of(fields);
      var fieldMap = new HashMap<String, FieldIndex>();
      for (var i = 0; i < fields.length; i++) {
        Field field = fields[i];
        var result = fieldMap.put(field.name, new FieldIndex(field, i));
        if (result != null) {
          throw new IllegalArgumentException("multiple fields with the same name " + field.name);
        }
      }
      this.fields = fieldList;
      this.fieldMap = fieldMap;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof FieldMap fieldMap &&
          fields.equals(fieldMap.fields);
    }

    @Override
    public int hashCode() {
      return fields.hashCode();
    }

    public List<Field> fields() {
      //noinspection AssignmentOrReturnOfFieldWithMutableType
      return fields;
    }

    public Field field(String name) {
      requireNonNull(name);
      var fieldIndex = fieldMap.get(name);
      if (fieldIndex == null) {
        throw new IllegalStateException("unknown field " + name);
      }
      return fieldIndex.field;
    }

    public int fieldIndex(String name) {
      requireNonNull(name);
      var fieldIndex = fieldMap.get(name);
      if (fieldIndex == null) {
        return -1;
      }
      return fieldIndex.index;
    }
  }

  record Field(String name, Layout layout) {
    public Field {
      requireNonNull(name);
      requireNonNull(layout);
    }

    @Override
    public String toString() {
      return "field(" + name + ", " + layout + ")";
    }
  }
}