package com.github.forax.tomahawk.blackpanda;

import com.github.forax.tomahawk.schema.CSV;
import com.github.forax.tomahawk.schema.JSON;
import com.github.forax.tomahawk.schema.Layout;
import com.github.forax.tomahawk.schema.Layout.ListLayout;
import com.github.forax.tomahawk.schema.Layout.PrimitiveLayout;
import com.github.forax.tomahawk.schema.Layout.StructLayout;
import com.github.forax.tomahawk.vec.ListVec;
import com.github.forax.tomahawk.vec.StructVec;
import com.github.forax.tomahawk.vec.U16Vec;
import com.github.forax.tomahawk.vec.U1Vec;
import com.github.forax.tomahawk.vec.U32Vec;
import com.github.forax.tomahawk.vec.U64Vec;
import com.github.forax.tomahawk.vec.U8Vec;
import com.github.forax.tomahawk.vec.ValuesBox;
import com.github.forax.tomahawk.vec.Vec;
import com.github.forax.tomahawk.vec.VecOp;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;

import static com.github.forax.tomahawk.schema.Layout.int32;
import static java.util.Objects.requireNonNull;

public record DB(Path directory, String name) {
  public Table table(String name) {
    requireNonNull(name);
    return table(name, Layout.struct(true));
  }

  public Table table(String name, StructLayout structLayout) {
    requireNonNull(name);
    requireNonNull(structLayout);
    return new Table(directory.resolve(name + ".table"), name, structLayout);
  }


  public static DB of(Path directory, String name) {
    requireNonNull(directory);
    requireNonNull(name);
    return new DB(directory, name);
  }

  public class Table {
    private final Path directory;
    private final String name;

    private StructLayout structLayout;
    private StructVec structVec;  // may be null
    private final ArrayList<Col> cols = new ArrayList<>();

    Table(Path directory, String name, StructLayout structLayout) {
      this.directory = directory;
      this.name = name;
      this.structLayout = structLayout;
    }

    public long length() {
      return structVec == null? 0: structVec.length();
    }

    public Col col(String name) {
      var index = structLayout.fieldIndex(name);
      if (index == -1) {
        throw new IllegalStateException("unknown column " + name);
      }
      return cols.get(index);
    }

    public Col addCol(String name, Layout layout) {
      structLayout = addColumn(structLayout, name, layout);
      var columnVec = (Vec) null;
      if (structVec != null) {
        structVec = addColumnVec(structVec, name, layout);
        columnVec = structVec.fields().get(this.structLayout.fieldIndex(name));
      }
      return new Col(this, name, layout, columnVec);
    }

    private StructVec addColumnVec(StructVec structVec, String name, Layout layout) {
      Vec vec;
      try {
        vec = Layout.map(directory, name, layout);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      var size = structVec.fields().size();
      return structVec.splice(size, size, vec);
    }

    public Col addCol(String name) {
      return addCol(name, Layout.string(true));
    }

    public void addCols(String... names) {
      for(var name: names) {
        addCol(name);
      }
    }

    public void importCSV(Path path) {
      try {
        CSV.fetch(path, structLayout, directory, name);
        structVec = (StructVec) Layout.map(directory, name, structLayout);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public void importJSON(Path path) {
      try {
        JSON.fetch(path, structLayout, directory, name);
        structVec = (StructVec) Layout.map(directory, name, structLayout);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    private StructLayout addColumn(StructLayout structLayout, String name, Layout layout) {
      if (layout.fieldIndex(name) != -1) {
        throw new IllegalStateException("there is already a column with the name " + name);
      }
      var fields = new ArrayList<>(structLayout.fields());
      fields.add(Layout.field(name, layout));
      return Layout.struct(structLayout.nullable(), fields.toArray(Layout.Field[]::new));
    }
  }

  public class Col {
    private final Table table;
    private final String name;
    private final Layout layout;
    private Vec vec;  // may be null

    private Col(Table table, String name, Layout layout, Vec vec) {
      this.table = table;
      this.name = name;
      this.layout = layout;
    }

    public long length() {
      return vec == null ? 0 : vec.length();

    }

    public void set(long index, Object value) {
      if (vec == null) {
        throw new IllegalStateException("column " + name + " is empty");
      }
      setObject(layout, vec, index, value);
    }

    public Object get(long index) {
      if (vec == null) {
        throw new IllegalStateException("column " + name + " is empty");
      }
      return getObject(layout, vec, index);
    }

    public Col applyInt(Col col, String name, IntBinaryOperator op) {
      if (vec == null) {
        throw new IllegalStateException("column " + name + " is empty");
      }
      if (col.vec == null) {
        throw new IllegalStateException("column " + col.name + " is empty");
      }
      var newCol = table.addCol(name, int32(false));
      if (newCol.vec == null) {
        throw new IllegalStateException("table " + table.name + " is empty");
      }
      VecOp.of(null).applyInt(newCol.vec, vec, col.vec, op::applyAsInt);
      return newCol;
    }


    private static Object getObject(Layout layout, Vec vec, long index) {
      if (layout instanceof PrimitiveLayout primitiveLayout) {
        return getObjectPrimitive(primitiveLayout, vec, index);
      }
      if (layout instanceof ListLayout listLayout) {
        return getObjectList(listLayout, vec, index);
      }
      if (layout instanceof StructLayout structLayout) {
        return getObjectStruct(structLayout, vec, index);
      }
      throw new AssertionError("unknown layout");
    }

    private static Object getObjectPrimitive(PrimitiveLayout primitiveLayout, Vec vec, long index) {
      if (vec.isNull(index)) {
        return null;
      }
      return switch (primitiveLayout.kind()) {
        case u1 -> ((U1Vec) vec).getBoolean(index);
        case byte8 -> ((U8Vec) vec).getByte(index);
        case short16 -> ((U16Vec) vec).getShort(index);
        case char16 -> ((U16Vec) vec).getChar(index);
        case int32 -> ((U32Vec) vec).getInt(index);
        case float32 -> ((U32Vec) vec).getFloat(index);
        case long64 -> ((U64Vec) vec).getLong(index);
        case double64 -> ((U64Vec) vec).getDouble(index);
      };
    }

    private static Object getObjectList(ListLayout listLayout, Vec vec, long index) {
      var listVec = (ListVec<?>) vec;
      if (listLayout.dataType() == String.class) {
        return listVec.getString(index);
      }
      var box = new ValuesBox();
      listVec.getValues(index, box);
      if (!box.validity) {
        return null;
      }
      var data = listVec.element();
      var element = listLayout.element();
      if (element instanceof Layout.PrimitiveLayout primitiveLayout) {
        return switch (primitiveLayout.kind()) {
          case u1 -> box.booleanArray((U1Vec) data);
          case byte8 -> box.byteArray((U8Vec) data);
          case short16 -> box.shortArray((U16Vec) data);
          case char16 -> box.charArray((U16Vec) data);
          case int32 -> box.intArray((U32Vec) data);
          case float32 -> box.floatArray((U32Vec) data);
          case long64 -> box.longArray((U64Vec) data);
          case double64 -> box.doubleArray((U64Vec) data);
        };
      }
      return box.offsets().mapToObj(_index -> getObject(element, data, _index)).toArray();
    }

    private static Object getObjectStruct(StructLayout structLayout, Vec vec, long index) {
      var structVec = (StructVec) vec;
      if (structVec.isNull(index)) {
        return null;
      }
      var fields = structLayout.fields();
      return new CompactMap(structLayout, IntStream.range(0, structVec.fields().size())
          .mapToObj(i -> getObject(fields.get(i).layout(), structVec.fields().get(i), index))
          .toArray());
    }
  }

  private static void setObject(Layout layout, Vec vec, long index, Object value) {
    if (value == null) {
      vec.setNull(index);
      return;
    }
    if (layout instanceof PrimitiveLayout primitiveLayout) {
      setObjectPrimitive(primitiveLayout, vec, index, value);
      return;
    }
    if (layout instanceof ListLayout listLayout) {
      setObjectList(listLayout, vec, index, value);
      return;
    }
    if (layout instanceof StructLayout structLayout) {
      setObjectStruct(structLayout, vec, index, value);
      return;
    }
    throw new AssertionError("unknown layout");
  }

  private static void setObjectPrimitive(PrimitiveLayout primitiveLayout, Vec vec, long index, Object value) {
    switch (primitiveLayout.kind()) {
      case u1 -> ((U1Vec) vec).setBoolean(index, (boolean) value);
      case byte8 -> ((U8Vec) vec).setByte(index, (byte) value);
      case short16 -> ((U16Vec) vec).setShort(index, (short) value);
      case char16 -> ((U16Vec) vec).setChar(index, (char) value);
      case int32 -> ((U32Vec) vec).setInt(index, (int) value);
      case float32 -> ((U32Vec) vec).setFloat(index, (float) value);
      case long64 -> ((U64Vec) vec).setLong(index, (long) value);
      case double64 -> ((U64Vec) vec).setDouble(index, (double) value);
    }
  }

  private static void setObjectList(ListLayout listLayout, Vec vec, long index, Object value) {
    throw new UnsupportedOperationException("NYI");
  }

  private static void setObjectStruct(StructLayout structLayout, Vec vec, long index, Object value) {
    throw new UnsupportedOperationException("NYI");
  }
}
