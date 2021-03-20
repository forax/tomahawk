package com.github.forax.tomahawk.schema;

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
import com.github.forax.tomahawk.vec.Vec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class Table {
  public static Vec map(Path directory, String name) throws IOException {
    var layout = Layout.load(directory.resolve(name + "_metadata.txt"));
    return map(directory, name, layout);
  }

  public static Vec map(Path directory, String name, Layout layout) throws IOException {
    if (layout instanceof PrimitiveLayout primitiveLayout) {
      return mapPrimitive(directory, name, primitiveLayout);
    }
    if (layout instanceof ListLayout listLayout) {
      return mapList(directory, name, listLayout);
    }
    if (layout instanceof StructLayout structLayout) {
      return mapStruct(directory, name, structLayout);
    }
    throw new AssertionError("unknown layout");
  }

  private static Vec mapPrimitive(Path directory, String name, PrimitiveLayout primitiveLayout) throws IOException {
    var validity = primitiveLayout.nullable()? mapValidityVec(directory, name): null;
    var suffix = primitiveLayout.toString();
    var dataPath = directory.resolve(name + "_" + suffix + ".tmhk");
    return switch(primitiveLayout.kind()) {
      case u1 -> U1Vec.map(validity, dataPath);
      case byte8 -> U8Vec.map(validity, dataPath);
      case short16, char16 -> U16Vec.map(validity, dataPath);
      case int32, float32 -> U32Vec.map(validity, dataPath);
      case long64 , double64 -> U64Vec.map(validity, dataPath);
    };
  }

  private static StructVec mapStruct(Path directory, String name, StructLayout structLayout) throws IOException {
    var structName = name + "_struct";
    var validity = structLayout.nullable()? mapValidityVec(directory, structName): null;
    var fieldVecs = new ArrayList<Vec>();
    for (var entry : structLayout.fields().entrySet()) {
      var fieldName = entry.getKey();
      var fieldLayout = entry.getValue();
      fieldVecs.add(map(directory, structName + "-" + fieldName, fieldLayout));
    }
    return StructVec.from(validity, fieldVecs);
  }

  private static ListVec<?> mapList(Path directory, String name, ListLayout listLayout) throws IOException {
    var listName = name + "_list";
    var validity = listLayout.nullable()? mapValidityVec(directory, listName): null;
    var offset = mapOffsetVec(directory, listName);
    var data = map(directory, listName, listLayout.element());
    return ListVec.from(validity, offset, data);
  }

  private static U1Vec mapValidityVec(Path directory, String name) throws IOException {
    return U1Vec.map(null, directory.resolve(name + "_validity.tmhk"));
  }

  private static U32Vec mapOffsetVec(Path directory, String name) throws IOException {
    return U32Vec.map(null, directory.resolve(name + "_offset.tmhk"));
  }

  public static Vec.BaseBuilder<?> builder(Path directory, String name, Layout layout) throws IOException {
    if (layout instanceof PrimitiveLayout primitiveLayout) {
      return builderPrimitive(directory, name, primitiveLayout);
    }
    if (layout instanceof ListLayout listLayout) {
      return builderList(directory, name, listLayout);
    }
    if (layout instanceof StructLayout structLayout) {
      return builderStruct(directory, name, structLayout);
    }
    throw new AssertionError("unknown layout");
  }

  private static Vec.BaseBuilder<?> builderPrimitive(Path directory, String name, PrimitiveLayout primitiveLayout) throws IOException {
    var validity = primitiveLayout.nullable()? createValidityBuilder(directory, name): null;
    var suffix = primitiveLayout.toString();
    var dataPath = directory.resolve(name + "_" + suffix + ".tmhk");
    return switch(primitiveLayout.kind()) {
      case u1 -> U1Vec.builder(validity, dataPath);
      case byte8 -> U8Vec.builder(validity, dataPath);
      case short16, char16 -> U16Vec.builder(validity, dataPath);
      case int32, float32 -> U32Vec.builder(validity, dataPath);
      case long64 , double64 -> U64Vec.builder(validity, dataPath);
    };
  }

  private static ListVec.Builder<?, ? extends Vec.BaseBuilder<?>> builderList(Path directory, String name, ListLayout listLayout) throws IOException {
    var listName = name + "_list";
    var validity = listLayout.nullable()? createValidityBuilder(directory, listName): null;
    var offset = createOffsetBuilder(directory, listName);
    var data = builder(directory, listName, listLayout.element());
    return ListVec.builder(validity, offset, data);
  }

  private static StructVec.Builder builderStruct(Path directory, String name, StructLayout structLayout) throws IOException {
    var structName = name + "_struct";
    var validity = structLayout.nullable()? createValidityBuilder(directory, structName): null;
    var fieldBuilders = new ArrayList<Vec.BaseBuilder<?>>();
    for (var entry : structLayout.fields().entrySet()) {
      var fieldName = entry.getKey();
      var fieldLayout = entry.getValue();
      fieldBuilders.add(builder(directory, structName + "-" + fieldName, fieldLayout));
    }
    return StructVec.builder(validity, fieldBuilders);
  }

  private static U1Vec.Builder createValidityBuilder(Path directory, String name) throws IOException {
    return U1Vec.builder(null, directory.resolve(name + "_validity.tmhk"));
  }

  private static U32Vec.Builder createOffsetBuilder(Path directory, String name) throws IOException {
    return U32Vec.builder(null, directory.resolve(name + "_offset.tmhk"));
  }
}
