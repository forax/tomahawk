package com.github.forax.tomahawk.schema;

import com.github.forax.tomahawk.vec.ListVec;
import com.github.forax.tomahawk.vec.U16Vec;
import com.github.forax.tomahawk.vec.U8Vec;
import com.github.forax.tomahawk.vec.ValuesBox;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.LongStream;

import static com.github.forax.tomahawk.schema.Layout.byte8;
import static com.github.forax.tomahawk.schema.Layout.field;
import static com.github.forax.tomahawk.schema.Layout.list;
import static com.github.forax.tomahawk.schema.Layout.string;
import static com.github.forax.tomahawk.schema.Layout.struct;
import static com.github.forax.tomahawk.schema.Layout.u1;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JSONTest {
  @Test
  public void fetch() throws IOException {
    var json = """
        [
          { "name": "Bob", "married": false, "email": "bob@bobstore.com" },
          { "name": "Ana", "married": false, "email": "ana21@zemail.com" },
          { "name": "Jay", "married": true, "email": "humble_jay@foo.com" }
        ]
        """;
    var layout = struct(false,
        field("name",      string(false)),
        field("married",   u1(false)),
        field("email",     string(false))
    );
    var directory = createTempDirectory("data");
    Closeable andClean = () -> {
      for (var temp : list(directory).toList()) {
        delete(temp);
      }
      delete(directory);
    };
    try(andClean) {
      JSON.fetch(json, layout, directory, "employee");
    }
  }

  @Test
  public void fetchAndLoad() throws IOException {
    var json = """
        [
          {
            "name": "Joe",
            "age": 18,
            "phones": [
              "555-111-1111",
              "555-222-2222"
            ]
          }, {
            "name": "Jack",
            "age": 37,
            "phones": [ "555-333-3333" ]
          }
        ]
        """;
    var layout =
        struct(false,
            field("name",    string(false)),
            field("age",     byte8(false)),
            field("phones",  list(false, string(false)))
        );
    var directory = createTempDirectory("persons");
    Closeable andClean = () -> {
      for (var temp : list(directory).toList()) {
        delete(temp);
      }
      delete(directory);
    };
    try(andClean) {
      JSON.fetch(json, layout, directory, "persons");
      var person = Table.map(directory, "persons", layout).asStructVec();
      var name = person.fields().get(0).asListVec(U16Vec.class);
      var age = person.fields().get(1).asVec(U8Vec.class);
      var phones = person.fields().get(2).asListVec(ListVec.class);

      var names = LongStream.range(0, name.length()).mapToObj(name::getString).toList();
      assertEquals(List.of("Joe", "Jack"), names);

      var ages = LongStream.range(0, name.length()).mapToObj(age::getByte).toList();
      assertEquals(List.of((byte) 18, (byte) 37), ages);

      var valuesBox = new ValuesBox();
      phones.getValues(0, valuesBox);
      var joePhones = valuesBox.offsets().mapToObj(phones.data()::getString).toList();
      phones.getValues(1, valuesBox);
      var jackPhones = valuesBox.offsets().mapToObj(phones.data()::getString).toList();
      assertEquals(List.of("555-111-1111", "555-222-2222"), joePhones);
      assertEquals(List.of("555-333-3333"), jackPhones);
    }
  }
}