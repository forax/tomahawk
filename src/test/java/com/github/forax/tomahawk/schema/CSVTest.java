package com.github.forax.tomahawk.schema;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;

import static com.github.forax.tomahawk.schema.Layout.double64;
import static com.github.forax.tomahawk.schema.Layout.field;
import static com.github.forax.tomahawk.schema.Layout.int32;
import static com.github.forax.tomahawk.schema.Layout.string;
import static com.github.forax.tomahawk.schema.Layout.struct;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.list;

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
        field("Year", int32(false)),
        field("Make", string(false)),
        field("Model", string(false)),
        field("Description", string(true)),
        field("Price", double64(true))
    );
    var directory = createTempDirectory("data");
    Closeable defer = () -> {
      for (var temp : list(directory).toList()) {
        delete(temp);
      }
      delete(directory);
    };
    try(defer) {
      CSV.fetch(csv, layout, directory, "cars");
    }
  }
}