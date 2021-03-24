package com.github.forax.tomahawk.schema;

import com.github.forax.tomahawk.schema.Layout.StructLayout;
import com.github.forax.tomahawk.vec.U16Vec;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;

import static com.github.forax.tomahawk.schema.Layout.double64;
import static com.github.forax.tomahawk.schema.Layout.field;
import static com.github.forax.tomahawk.schema.Layout.int32;
import static com.github.forax.tomahawk.schema.Layout.string;
import static com.github.forax.tomahawk.schema.Layout.struct;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("static-method")
public class CSVTest {
  @Test
  public void fetch() throws IOException {
    var csv = """
        Year,Make,Model,Description,Price
        1997,Ford,E350,"ac, abs, moon",3000.00
        1999,Chevy,"Venture ""Extended Edition""\","",4900.00
        1999,Chevy,"Venture ""Extended Edition, Very Large""\",,5000.00
        1996,Jeep,Grand Cherokee,"MUST SELL!
        air, moon roof, loaded",4799.00
        """;
    var layout = struct(false,
        field("Year",        int32(false)),
        field("Make",        string(false)),
        field("Model",       string(false)),
        field("Description", string(true)),
        field("Price",       double64(true))
    );
    var directory = createTempDirectory("data");
    Closeable andClean = () -> {
      for (var temp : list(directory).toList()) {
        delete(temp);
      }
      delete(directory);
    };
    try(andClean) {
      CSV.fetch(csv, layout, directory, "cars");
    }
  }

  @Test
  public void fetchAndLoad() throws IOException {
    var csv = """
        Name,Job Title,Address,State,City
        "Doe, John",Designer,325 Pine Street,,Seattle
        "Green, Edward",Developer,110 Pike Street,WA,Seattle
        """;
    var layout = Layout.parse("""
        struct(false,
            field("Name",      string(false)),
            field("Job Title", string(false)),
            field("Address",   string(false)),
            field("State",     string(true)),
            field("City",      string(false))
        )
        """);
    var directory = createTempDirectory("jobs");
    Closeable andClean = () -> {
      for (var temp : list(directory).toList()) {
        delete(temp);
      }
      delete(directory);
    };
    try(andClean) {
      CSV.fetch(csv, (StructLayout) layout, directory, "jobs");
      var vec = Layout.map(directory, "jobs").asStruct();
      var name = vec.fields().get(layout.fieldIndex("Name")).asListOf(U16Vec.class);
      var state = vec.fields().get(layout.fieldIndex("State")).asListOf(U16Vec.class);

      var names = LongStream.range(0, name.length()).mapToObj(name::getString).toList();
      assertEquals(List.of("Doe, John", "Green, Edward"), names);

      var states = LongStream.range(0, name.length()).mapToObj(state::getString).toList();
      assertEquals(Arrays.asList(null, "WA"), states);
    }
  }
}