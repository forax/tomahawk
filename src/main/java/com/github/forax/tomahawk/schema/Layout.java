package com.github.forax.tomahawk.schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.*;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

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
  default Map<String, Layout> fields() {
    return Map.of();
  }
  default Layout field(String name) {
    throw new IllegalArgumentException("unknown field " + name);
  }
  default Layout element() {
    throw new IllegalArgumentException("no element");
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
    return new StructLayout(nullable, Arrays.stream(fields).collect(toMap(Field::name, Field::layout, (_1, _2) -> null, LinkedHashMap::new)));
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
      if (structLayout.fields.isEmpty()) {
        return "struct(" + structLayout.nullable + ")";
      }
      var newSpace = space + "   ";
      return structLayout.fields.entrySet().stream()
          .map(e -> newSpace + "field(\"" + e.getKey() + "\", " + toString(newSpace, e.getValue()) + ")")
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

  record StructLayout(boolean nullable, Map<String, Layout> fields) implements Layout {
    public StructLayout {
      fields = new LinkedHashMap<>(fields);
    }

    @Override
    public Layout field(String name) {
      var layout = fields.get(name);
      if (layout == null) {
        throw new IllegalArgumentException("unknown field " + name);
      }
      return layout;
    }

    @Override
    public Map<String, Layout> fields() {
      return unmodifiableMap(fields);
    }

    @Override
    public String toString() {
      return Layout.toString("", this);
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