package com.github.forax.tomahawk.schema;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.github.forax.tomahawk.schema.Layout.ListLayout;
import com.github.forax.tomahawk.schema.Layout.PrimitiveLayout;
import com.github.forax.tomahawk.schema.Layout.StructLayout;
import com.github.forax.tomahawk.vec.ListVec;
import com.github.forax.tomahawk.vec.StructVec;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.forax.tomahawk.vec.StructVec.RowBuilder;
import com.github.forax.tomahawk.vec.U16Vec;
import com.github.forax.tomahawk.vec.U1Vec;
import com.github.forax.tomahawk.vec.U32Vec;
import com.github.forax.tomahawk.vec.U64Vec;
import com.github.forax.tomahawk.vec.U8Vec;
import com.github.forax.tomahawk.vec.Vec;
import com.github.forax.tomahawk.vec.Vec.BaseBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.fasterxml.jackson.core.JsonToken.END_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.VALUE_STRING;

public final class CSV {
  private CSV() {
    throw new AssertionError();
  }

  public static void fetch(String text, StructLayout layout, Path directory, String name) throws IOException {
    fetch(new StringReader(text), layout, directory, name);
  }

  public static void fetch(Path path, StructLayout layout, Path directory, String name) throws IOException {
    try (var reader = Files.newBufferedReader(path)) {
      fetch(reader, layout, directory, name);
    }
  }

  private record Column(BaseBuilder<?> builder, Layout layout) {}

  public static void fetch(Reader reader, StructLayout layout, Path directory, String name) throws IOException {
    try(var structBuilder = (StructVec.Builder) TableImpl.builder(directory, name, layout)) {
      var fieldBuilders = structBuilder.fieldBuilders();
      var columnMap = new HashMap<String, Column>();
      var index = 0;
      for (var field : layout.fields()) {
        var fieldName = field.name();
        var fieldLayout = field.layout();
        columnMap.put(fieldName, new Column(fieldBuilders.get(index++), fieldLayout));
      }

      var factory = CsvFactory.builder().build();
      try (var parser = factory.createParser(reader)) {
        parser.setSchema(CsvSchema.emptySchema());

        var headers = parseHeaders(parser);
        var columns = new ArrayList<Column>();
        for (var header : headers) {
          var column = columnMap.get(header);
          if (column == null) {
            throw new JsonParseException(parser, "header " + header + " has no layout among " + layout.fields());
          }
          columns.add(column);
        }

        parseRows(parser, structBuilder, columns);
      }
    }
  }

  private static void parseRows(CsvParser parser, StructVec.Builder structBuilder, ArrayList<Column> columns) throws IOException {
    expect(parser, START_ARRAY);
    for(;;) {
      try {
        structBuilder.appendRow(rowBuilder -> {
          try {
            for(var column: columns) {
              var token = parser.nextToken();
              if (token == END_ARRAY) {
                throw new UncheckedIOException(
                    new JsonParseException(parser, "not enough data, the headers defines " + columns.size() + " columns"));
              }
              if (token != VALUE_STRING) {
                throw new UncheckedIOException(new JsonParseException(parser, "unknown data " + token));
              }
              var text = parser.getText();
              //System.out.println("parse data " + token + " " + text);
              insertData(rowBuilder, column.builder, column.layout, text);
            }
          } catch(IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      } catch(UncheckedIOException e) {  // unpack UncheckedIOException
        throw e.getCause();
      }
      expect(parser, END_ARRAY);
      var token = parser.nextToken();
      if (token != START_ARRAY) {
        return;
      }
    }
  }

  private static char parseChar(String text) {
    if (text.length() != 1) {
      throw new NumberFormatException(text + " is not a valid character");
    }
    return text.charAt(0);
  }

  private static void insertData(RowBuilder rowBuilder, Vec.BaseBuilder<?> builder, Layout layout, String text) {
    if (text.isEmpty()) {
      rowBuilder.appendNull(builder);
      return;
    }
    if (layout instanceof PrimitiveLayout primitiveLayout) {
      switch(primitiveLayout.kind()) {
        case u1 -> rowBuilder.appendBoolean((U1Vec.Builder) builder, Boolean.parseBoolean(text));
        case byte8 -> rowBuilder.appendByte((U8Vec.Builder) builder, Byte.parseByte(text));
        case short16 -> rowBuilder.appendShort((U16Vec.Builder) builder, Short.parseShort(text));
        case char16 -> rowBuilder.appendChar((U16Vec.Builder) builder, parseChar(text));
        case int32 -> rowBuilder.appendInt((U32Vec.Builder) builder, Integer.parseInt(text));
        case float32 -> rowBuilder.appendFloat((U32Vec.Builder) builder, Float.parseFloat(text));
        case long64 -> rowBuilder.appendLong((U64Vec.Builder) builder, Long.parseLong(text));
        case double64 -> rowBuilder.appendDouble((U64Vec.Builder) builder, Double.parseDouble(text));
        default -> throw new AssertionError();
      }
      return;
    }
    if (layout instanceof ListLayout listLayout) {
      rowBuilder.appendString((ListVec.Builder<U16Vec, U16Vec.Builder>) builder, text);  //FIXME
      return;
    }
    throw new IllegalStateException("invalid layout " + layout);
  }

  private static void expect(CsvParser parser, JsonToken expectedToken) throws IOException {
    var token = parser.nextToken();
    if (token != expectedToken) {
      throw new JsonParseException(parser, "expected token " + expectedToken + " but found " + token);
    }
  }

  private static List<String> parseHeaders(CsvParser parser) throws IOException {
    expect(parser, START_ARRAY);
    var headers = new ArrayList<String>();
    for(;;) {
      var token = parser.nextToken();
      switch(token) {
        case VALUE_STRING -> headers.add(parser.getText());
        case END_ARRAY -> { return headers; }
        default -> throw new JsonParseException(parser, "unexpected token " + token);
      }
    }
  }
}
