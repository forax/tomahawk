package com.github.forax.tomahawk.schema;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.char16;
import static com.github.forax.tomahawk.schema.Layout.PrimitiveLayout.Kind.u1;

public class JSON {
  private JSON() {
    throw new AssertionError();
  }

  public static void fetch(String text, Layout layout, Path directory, String name) throws IOException {
    fetch(new StringReader(text), layout, directory, name);
  }

  public static void fetch(Path path, Layout layout, Path directory, String name) throws IOException {
    try (var reader = Files.newBufferedReader(path)) {
      fetch(reader, layout, directory, name);
    }
  }

  public static void fetch(Reader reader, Layout layout, Path directory, String name) throws IOException {
    try(var vecBuilder = TableImpl.builder(directory, name, layout)) {
      var factory = JsonFactory.builder().build();
      try (var parser = factory.createParser(reader)) {
        var token = parser.nextToken();
        if (token == JsonToken.START_ARRAY) {
          parseArray(parser, vecBuilder, layout);
          return;
        }
        if (token == JsonToken.START_OBJECT) {
          if (!(layout instanceof StructLayout structLayout)) {
            throw new JsonParseException(parser, "found an object but layout is not a StructLayout " + layout);
          }
          parseObject(parser, vecBuilder, structLayout);
        }
      }
    }

    Layout.save(directory.resolve(name + "_metadata.txt"), layout);
  }

  private static void parseArray(JsonParser parser, Vec.BaseBuilder<?> builder, Layout layout) throws IOException {
    for(;;) {
      var token = parser.nextToken();
      if (token == END_ARRAY) {
        return;
      }
      parseValue(parser, builder, layout, token);
    }
  }

  private static void parseValue(JsonParser parser, Vec.BaseBuilder<?> builder, Layout layout, JsonToken token) throws IOException {
    switch(token) {
      case VALUE_NULL -> {
        if (!layout.nullable()) {
          throw new JsonParseException(parser, "found null but layout is not nullable " + layout);
        }
        builder.appendNull();
      }
      case VALUE_TRUE, VALUE_FALSE -> {
        if (!(layout instanceof PrimitiveLayout primitiveLayout) || primitiveLayout.kind() != u1) {
          throw new JsonParseException(parser, "found a boolean but layout is not a u1 " + layout);
        }
        var u1Builder = (U1Vec.Builder) builder;
        u1Builder.appendBoolean(token == JsonToken.VALUE_TRUE);
      }
      case VALUE_NUMBER_INT -> {
        if (!(layout instanceof PrimitiveLayout primitiveLayout)) {
          throw new JsonParseException(parser, "found an int but layout is not a primitive layout " + layout);
        }
        var kind = primitiveLayout.kind();
        switch(kind) {
          case byte8 -> ((U8Vec.Builder) builder).appendByte((byte) parser.getIntValue());
          case short16 -> ((U16Vec.Builder) builder).appendShort((short) parser.getIntValue());
          case int32 -> ((U32Vec.Builder) builder).appendInt(parser.getIntValue());
          case long64 -> ((U64Vec.Builder) builder).appendLong(parser.getIntValue());
          default -> throw new JsonParseException(parser, "found an int but layout is a " + kind);
        }
      }
      case VALUE_NUMBER_FLOAT -> {
        if (!(layout instanceof PrimitiveLayout primitiveLayout)) {
          throw new JsonParseException(parser, "found a floating point value but layout is not a primitive layout " + layout);
        }
        var kind = primitiveLayout.kind();
        switch(kind) {
          case float32 -> ((U32Vec.Builder) builder).appendFloat(parser.getFloatValue());
          case double64 -> ((U64Vec.Builder) builder).appendDouble(parser.getDoubleValue());
          default -> throw new JsonParseException(parser, "found a floating point value but layout is a " + kind);
        }
      }
      case VALUE_STRING -> {
        if (!(layout instanceof ListLayout listLayout)) {
          throw new JsonParseException(parser, "found a string but layout is not a ListLayout " + layout);
        }
        if (!(listLayout.element() instanceof PrimitiveLayout primitiveLayout) || primitiveLayout.kind() != char16) {
          throw new JsonParseException(parser, "found a string but element layout is not a char16 " + listLayout.element());
        }
        var listBuilder = (ListVec.Builder<?, ?>) builder;
        listBuilder.appendString(parser.getText());
      }
      case START_OBJECT -> {
        if (!(layout instanceof StructLayout structLayout)) {
          throw new JsonParseException(parser, "found an object but layout is not a StructLayout " + layout);
        }
        parseObject(parser, builder, structLayout);
      }
      case START_ARRAY -> {
        if (!(layout instanceof ListLayout listLayout)) {
          throw new JsonParseException(parser, "found an array but layout is not a ListLayout " + layout);
        }
        var listBuilder = (ListVec.Builder<?,?>) builder;
        try {
          listBuilder.appendValues(itemBuilder -> {
            try {
              parseArray(parser, itemBuilder, listLayout.element());
            } catch (IOException e) {
              throw new UncheckedIOException(e);  // tunnel exception
            }
          });
        } catch(UncheckedIOException e) {
          throw e.getCause();
        }
      }
      default -> throw new JsonParseException(parser, "invalid token " + token);
    }
  }

  private static void parseObject(JsonParser parser, Vec.BaseBuilder<?> builder, StructLayout structLayout) throws IOException {
    for(;;) {
      var token = parser.nextToken();
      switch(token) {
        case FIELD_NAME -> {
          var fieldName = parser.getCurrentName();
          var fieldIndex = structLayout.fieldIndex(fieldName);
          if (fieldIndex == -1) {
            throw new JsonParseException(parser, "found field " + fieldName + " but struct layout has no field with that name");
          }
          var fieldLayout = structLayout.fields().get(fieldIndex).layout();
          var fieldBuilder = ((StructVec.Builder) builder).fieldBuilders().get(fieldIndex);
          var valueToken = parser.nextToken();
          parseValue(parser, fieldBuilder, fieldLayout, valueToken);
        }
        case END_OBJECT -> { return; }
        default -> throw new JsonParseException(parser, "invalid token " + token);
      }
    }
  }
}
