package com.github.forax.tomahawk.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.forax.tomahawk.schema.Layout.field;
import static com.github.forax.tomahawk.schema.Layout.list;
import static com.github.forax.tomahawk.schema.Layout.string;
import static com.github.forax.tomahawk.schema.Layout.struct;
import static com.github.forax.tomahawk.schema.Layout.u1;
import static com.github.forax.tomahawk.schema.Layout.int32;
import static com.github.forax.tomahawk.schema.Layout.byte8;

@SuppressWarnings("static-method")
public class LayoutTest {
  @Test
  public void simple() {
    var layout =
        struct(true,
            field("name", string(false)),
            field("age", byte8(true)),
            field("user", struct(false,
                field("id", int32(false)),
                field("admin", u1(false))
            )),
            field("addresses", list(true, string(false)))
        );

    assertEquals(string(false), layout.field("name"));
    assertEquals(byte8(true), layout.field("age"));
    assertEquals(struct(false,
        field("id", int32(false)),
        field("admin", u1(false))
      ), layout.field("user"));
    assertEquals(int32(false), layout.field("user").field("id"));
    assertEquals(u1(false), layout.field("user").field("admin"));
    assertEquals(Map.of("id", int32(false), "admin", u1(false)), layout.field("user").fields());
    assertEquals(layout.field("addresses"), list(true, string(false)));
  }
}