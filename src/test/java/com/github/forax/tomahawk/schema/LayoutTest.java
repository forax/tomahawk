package com.github.forax.tomahawk.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

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

    assertEquals(string(false), layout.field("name").layout());
    assertEquals(byte8(true), layout.field("age").layout());
    assertEquals(struct(false,
        field("id", int32(false)),
        field("admin", u1(false))
      ), layout.field("user").layout());
    assertEquals(int32(false), layout.field("user").layout().field("id").layout());
    assertEquals(u1(false), layout.field("user").layout().field("admin").layout());
    assertEquals(
        List.of(field("id", int32(false)), field("admin", u1(false))),
        layout.field("user").layout().fields());
    assertEquals(list(true, string(false)), layout.field("addresses").layout());
  }

  @Test
  public void parser() {
    var text = """
        struct(true,
            field("name", string(false)),
            field("age", byte8(true)),
            field("user", struct(false,
                field("id", int32(false)),
                field("admin", u1(false))
            )),
            field("addresses", list(true, string(false)))
        )
        """;

    var layout = Layout.parse(text);
    assertEquals(struct(true,
        field("name", string(false)),
        field("age", byte8(true)),
        field("user", struct(false,
            field("id", int32(false)),
            field("admin", u1(false))
        )),
        field("addresses", list(true, string(false)))
    ), layout);

    assertEquals(string(false), layout.field("name").layout());
    assertEquals(byte8(true), layout.field("age").layout());
    assertEquals(struct(false,
        field("id", int32(false)),
        field("admin", u1(false))
    ), layout.field("user").layout());
    assertEquals(int32(false), layout.field("user").layout().field("id").layout());
    assertEquals(u1(false), layout.field("user").layout().field("admin").layout());
    assertEquals(
        List.of(field("id", int32(false)), field("admin", u1(false))),
        layout.field("user").layout().fields());
    assertEquals(list(true, string(false)), layout.field("addresses").layout());
  }
}