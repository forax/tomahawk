package com.github.forax.tomahawk.schema;

import com.github.forax.tomahawk.vec.ListVec;
import com.github.forax.tomahawk.vec.TextWrap;
import com.github.forax.tomahawk.vec.U16Vec;
import com.github.forax.tomahawk.vec.U1Vec;
import com.github.forax.tomahawk.vec.U8Vec;
import com.github.forax.tomahawk.vec.ValuesBox;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;

import static com.github.forax.tomahawk.schema.Layout.byte8;
import static com.github.forax.tomahawk.schema.Layout.field;
import static com.github.forax.tomahawk.schema.Layout.int32;
import static com.github.forax.tomahawk.schema.Layout.list;
import static com.github.forax.tomahawk.schema.Layout.string;
import static com.github.forax.tomahawk.schema.Layout.struct;
import static com.github.forax.tomahawk.schema.Layout.u1;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.list;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("static-method")
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
      var person = Layout.map(directory, "persons", layout).asStructVec();
      var name = person.fields().get(0).asListVec(U16Vec.class);
      var age = person.fields().get(1).asVec(U8Vec.class);
      var phones = person.fields().get(2).asListVec(ListVec.class);

      var names = LongStream.range(0, name.length()).mapToObj(name::getString).toList();
      assertEquals(List.of("Joe", "Jack"), names);

      var ages = LongStream.range(0, name.length()).mapToObj(age::getByte).toList();
      assertEquals(List.of((byte) 18, (byte) 37), ages);

      var valuesBox = new ValuesBox();
      phones.getValues(0, valuesBox);
      var joePhones = valuesBox.textWraps(phones.data()).map(TextWrap::toString).toList();
      phones.getValues(1, valuesBox);
      var jackPhones = valuesBox.textWraps(phones.data()).map(TextWrap::toString).toList();
      assertEquals(List.of("555-111-1111", "555-222-2222"), joePhones);
      assertEquals(List.of("555-333-3333"), jackPhones);
    }
  }

  @Test
  public void fetchAndLoadScottishParliamentMembers() throws IOException {
    // from https://data.parliament.scot/api/members
    var layout =
        struct(false,
            field("PersonID",  int32(false)),
            field("PhotoURL",  string(false)),
            field("Notes",     string(false)),
            field("BirthDate", string(false)),
            field("BirthDateIsProtected", u1(false)),
            field("ParliamentaryName",  string(false)),
            field("PreferredName",  string(false)),
            field("GenderTypeID",  byte8(false)),
            field("IsCurrent", u1(false))
        );
    var directory = createTempDirectory("scottish-parliament-members");
    Closeable andClean = () -> {
      for (var temp : list(directory).toList()) {
        delete(temp);
      }
      delete(directory);
    };
    try(var input = JSONTest.class.getResourceAsStream("/scottish-parliament-members.json");
        var reader = new InputStreamReader(requireNonNull(input), StandardCharsets.UTF_8);
        andClean) {
      JSON.fetch(reader, layout, directory, "scottish-parliament-members");
      var memberVec = Table.map(directory, "scottish-parliament-members", layout).asStructVec();

      // mask past member names
      var isCurrent = memberVec.fields().get(8).asVec(U1Vec.class);
      var preferredName = memberVec.fields().get(5).asListVec(U16Vec.class);
      preferredName = preferredName.withValidity(isCurrent);

      var names =
          preferredName.allTextWraps()
              .filter(Objects::nonNull)
              .limit(5)
              .map(TextWrap::toString)
              .toList();
      assertEquals(
          List.of("Constance, Angela", "Ewing, Annabelle", "Grahame, Christine", "Beamish, Claudia", "Smith, Elaine"),
          names);
    }
  }
}