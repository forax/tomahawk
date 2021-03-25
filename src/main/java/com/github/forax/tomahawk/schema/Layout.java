package com.github.forax.tomahawk.schema;

import com.github.forax.tomahawk.vec.Vec;

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

/**
 *
 */
public interface Layout /*permits Layout.PrimitiveLayout, Layout.ListLayout, Layout.StructLayout*/ {
  /**
   * Returns true if the current layout is a {@link PrimitiveLayout}
   * @return true if the current layout is a {@code PrimitiveLayout}
   */
  default boolean isPrimitive() {
    return this instanceof PrimitiveLayout;
  }

  /**
   * Returns true if he current layout is a {@link ListLayout}
   * @return true if he current layout is a {@code ListLayout}
   */
  default boolean isList() {
    return this instanceof ListLayout;
  }

  /**
   * Returns true if he current layout is a {@link StructLayout}
   * @return true if he current layout is a {@code StructLayout}
   */
  default boolean isStruct() {
    return this instanceof StructLayout;
  }

  /**
   * Returns true if the current layout is nullable
   * @return true if the current layout is nullable
   */
  boolean nullable();

  /**
   * return the fields of the {@link StructLayout} if the current layout is a struct layout
   * or an empty list otherwise
   * @return the fields of a {@link StructLayout}
   */
  default List<Field> fields() {
    return List.of();
  }

  /**
   * Returns the field of the {@link StructLayout} if the current layout is a struct layout
   * and the field exists or throw an exception
   * @param name the name of the field
   * @return the field of {@link StructLayout}
   * @throws IllegalStateException if there is no field named {@code name}
   */
  default Field field(String name) {
    throw new IllegalStateException("no field " + name);
  }

  /**
   * Returns the field index of the {@link StructLayout} if the current layout is a struct layout
   * @param name the name of the field
   * @return the index of the field or {@code -1} otherwise
   */
  default int fieldIndex(@SuppressWarnings("unused") String name) {
    return -1;
  }


  /**
   * Returns the element of the {@link ListLayout} if the current layout if a list layout
   * @return the element of the {@code ListLayout}
   */
  default Layout element() {
    throw new IllegalStateException("no element");
  }

  /**
   * Saves the textual representation of the current layout to a file
   * @param path the path of the file
   * @throws IOException if an io error occurs
   */
  default void saveTo(Path path) throws IOException {
    Files.writeString(path, toString());
  }

  /**
   * Loads a Layout from a file
   * @param path path to the file
   * @return the loaded layout
   * @throws IOException if an io error occurs
   */
  static Layout loadFrom(Path path) throws IOException {
    var text = Files.readString(path);
    return parse(text);
  }

  /**
   * Returns a layout from a text containing a textual representation of a Layout
   * @param text the text containing a textual representation of a Layout
   * @return a layout from a text containing a textual representation of a Layout
   */
  static Layout parse(String text) {
    return LayoutParser.parse(text);
  }


  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U1Vec one bit Vec}
   * of booleans
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing a booleans {@code Vec}
   */
  static PrimitiveLayout u1(boolean nullable) {
    return new PrimitiveLayout(nullable, u1);
  }

  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U8Vec 8 bits Vec} of bytes
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing a bytes {@code Vec}
   */
  static PrimitiveLayout byte8(boolean nullable) {
    return new PrimitiveLayout(nullable, byte8);
  }

  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U8Vec 16 bits Vec} of shorts
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing a shorts {@code Vec}
   */
  static PrimitiveLayout short16(boolean nullable) {
    return new PrimitiveLayout(nullable, short16);
  }

  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U16Vec 16 bits Vec} of chars
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing a chars {@code Vec}
   */
  static PrimitiveLayout char16(boolean nullable) {
    return new PrimitiveLayout(nullable, char16);
  }

  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U32Vec 32 bits Vec} of ints
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing an ints {@code Vec}
   */
  static PrimitiveLayout int32(boolean nullable) {
    return new PrimitiveLayout(nullable, int32);
  }

  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U32Vec 32 bits Vec} of floats
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing a floats {@code Vec}
   */
  static PrimitiveLayout float32(boolean nullable) {
    return new PrimitiveLayout(nullable, float32);
  }

  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U32Vec 32 bits Vec} of floats
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing a floats {@code Vec}
   */
  static PrimitiveLayout long64(boolean nullable) {
    return new PrimitiveLayout(nullable, long64);
  }

  /**
   * Returns a primitive layout corresponding to a {@link com.github.forax.tomahawk.vec.U64Vec 64 bits Vec} of doubles
   * @param nullable if the primitive layout represents a nullable {@link Vec}
   * @return a primitive layout representing a doubles {@code Vec}
   */
  static PrimitiveLayout double64(boolean nullable) {
    return new PrimitiveLayout(nullable, double64);
  }

  /**
   * Returns a list layout composed from an element layout
   * @param nullable if the lest layout represents a nullable {@link Vec}
   * @param layout the element layout of the list
   * @return a list layout composed from an element layout
   */
  static ListLayout list(boolean nullable, Layout layout) {
    return new ListLayout(nullable, layout);
  }

  /**
   * Returns a list representing a string of u16 characters
   *
   * This is semantically equivalent to
   * <pre>
   *   list(nullable, char16(false))
   * </pre>
   *
   * @param nullable true if the list is nullable
   * @return a list representing a string of u16 characters
   */
  static ListLayout string(boolean nullable) {
    return list(nullable, char16(false));
  }

  /**
   * Returns a field of a {@link StructLayout struct layout}
   * @param name the name of the field
   * @param layout the layout of the field
   * @return a field of a struct layout
   *
   * @see StructLayout
   */
  static Field field(String name, Layout layout) {
    return new Field(name, layout);
  }

  /**
   * Returns a struct layout from several fields
   *
   * @param nullable true if the struct is nullable
   * @param fields the field composing the struct
   * @return a struct layout from several fields
   */
  static StructLayout struct(boolean nullable, Field... fields) {
    return new StructLayout(nullable, fields);
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

  /**
   * Maps several columns from the name of the "table", a directory containing the different columns
   * and a Layout (that stores the hierarchy and the name of the files)
   *
   * @param directory the directory containing the table
   * @param name the name of the table
   * @param layout the layout of the table
   * @return a Vec able to load data from that table
   * @throws IOException if an io error occurs
   */
  static Vec map(Path directory, String name, Layout layout) throws IOException {
    return TableImpl.map(directory, name, layout);
  }

  /**
   * Maps several columns from the name of the "table", a directory containing the different columns
   * The layout is loaded from filesystem too.
   *
   * @param directory the directory containing all the file
   * @param name the name of the table
   * @return a Vec able to load data from that table
   * @throws IOException if an io error occurs
   *
   * @see #map(Path, String, Layout)
   */
  static Vec map(Path directory, String name) throws IOException {
    var layout = loadFrom(directory.resolve(name + "_metadata.txt"));
    return TableImpl.map(directory, name, layout);
  }

  static Vec.BaseBuilder<?> builder(Path directory, String name, Layout layout) throws IOException {
    return TableImpl.builder(directory, name, layout);
  }

  static Vec.BaseBuilder<?> builder(Path directory, String name) throws IOException {
    var layout = loadFrom(directory.resolve(name + "_metadata.txt"));
    return TableImpl.builder(directory, name, layout);
  }

  /**
   * A Layout for primitive values
   */
  record PrimitiveLayout(/**
                          * true if the layout is nullable
                         */
                         boolean nullable,

                         /**
                          * the type of the primitive values
                          */
                         Kind kind)
      implements Layout {

    /**
     * The type of the values of a {@link PrimitiveLayout}
     */
    enum Kind {
      /**
       * One bit boolean
       */
      u1,
      /**
       * 8 bits byte
       */
      byte8,
      /**
       * 16 bits short
       */
      short16,
      /**
       * 16 bits char
       */
      char16,
      /**
       * 32 bits int
       */
      int32,
      /**
       * 32 bits float
       */
      float32,
      /**
       * 64 bits long
       */
      long64,
      /**
       * 64 bits double
       */
      double64
    }

    /**
     * Create a primitive layout
     * @param nullable true if {@code null} is a possible value
     * @param kind the kind of primitive value allowed
     */
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

    @Override
    public String toString() {
      return fields.stream()
          .map(field -> "field(" + field.name() + ", " + field.layout + ")")
          .collect(joining(", ", "(", ")"));
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