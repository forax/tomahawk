package com.github.forax.tomahawk.redpanda;

import com.github.forax.tomahawk.schema.Layout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.forax.tomahawk.schema.Layout.*;
import static org.junit.jupiter.api.Assertions.*;

public class CompactMapTest {
  @Test
  public void fooAndBar() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertFalse(map.isEmpty()),
        () -> assertEquals(1, map.get("foo")),
        () -> assertEquals(2, map.get("bar")),
        () -> assertNull(map.get("hjdskhhds")),
        () -> assertEquals(1, map.getOrDefault("foo", 0)),
        () -> assertEquals(2, map.getOrDefault("bar", 0)),
        () -> assertEquals(0, map.getOrDefault("hjdskhhds", 0)),
        () -> assertTrue(map.containsKey("foo")),
        () -> assertTrue(map.containsKey("bar")),
        () -> assertFalse(map.containsKey("hjdskhhds")),
        () -> assertTrue(map.containsValue(1)),
        () -> assertTrue(map.containsValue(2)),
        () -> assertFalse(map.containsValue("hjdskhhds"))
    );
  }

  @Test
  public void empty() {
    var layout =
        struct(false);
    var map =  new CompactMap(layout, new Object[0]);
    assertAll(
        () -> assertEquals(0, map.size()),
        () -> assertTrue(map.isEmpty()),
        () -> assertNull(map.get("foo")),
        () -> assertNull( map.get("bar")),
        () -> assertNull(map.get("hjdskhhds")),
        () -> assertEquals(0, map.getOrDefault("foo", 0)),
        () -> assertEquals(0, map.getOrDefault("bar", 0)),
        () -> assertEquals(0, map.getOrDefault("hjdskhhds", 0)),
        () -> assertFalse(map.containsKey("foo")),
        () -> assertFalse(map.containsKey("bar")),
        () -> assertFalse(map.containsKey("hjdskhhds")),
        () -> assertFalse(map.containsValue(1)),
        () -> assertFalse(map.containsValue(2)),
        () -> assertFalse(map.containsValue("hjdskhhds"))
    );
  }

  @Test
  public void equalsAndHashCode() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    assertAll(
        () -> assertEquals(Map.of("foo", 1, "bar", 2), map),
        () -> assertEquals(Map.of("foo", 1, "bar", 2).hashCode(), map.hashCode())
    );
  }

  @Test
  public void put() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    map.put("foo", 3);
    assertEquals(3, map.get("foo"));
    assertThrows(IllegalStateException.class, () -> map.put("sdhsdhjs", 42));
  }

  @Test
  public void unsupported() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    assertAll(
        () -> assertThrows(UnsupportedOperationException.class, () -> map.remove("foo")),
        () -> assertThrows(UnsupportedOperationException.class, () -> map.remove("bar")),
        () -> assertThrows(UnsupportedOperationException.class, () -> map.remove("djkfhkjdshhs")),
        () -> assertThrows(UnsupportedOperationException.class, map::clear)
    );
  }

  @Test
  public void putAll() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    map.putAll(Map.of("bar", 7));
    assertEquals(7, map.get("bar"));
    assertThrows(IllegalStateException.class, () -> map.putAll(Map.of("sdhsdhjs", 42)));
  }


  @Test
  public void values() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    assertEquals(List.of(1, 2), map.values());
  }

  @Test
  public void keySet() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    assertEquals(Set.of("foo", "bar"), map.keySet());
  }

  @Test
  public void entrySet() {
    var layout =
        struct(false,
            field("foo", int32(false)),
            field("bar", string(false)));
    var map =  new CompactMap(layout, new Object[] { 1, 2 });
    assertEquals(Set.of(Map.entry("foo", 1), Map.entry("bar", 2)), map.entrySet());
  }
}