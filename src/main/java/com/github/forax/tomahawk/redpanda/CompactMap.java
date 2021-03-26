package com.github.forax.tomahawk.redpanda;

import com.github.forax.tomahawk.schema.Layout.StructLayout;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

record CompactMap(StructLayout layout, Object[]array) implements Map<String, Object> {
  @Override
  public int size() {
    return array.length;
  }

  @Override
  public boolean isEmpty() {
    return array.length == 0;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Map<?,?> map)) {
      return false;
    }
    if (map.size() != size()) {
      return false;
    }
    var fields = layout.fields();
    for(var i = 0; i < fields.size(); i++) {
      var key = fields.get(i).name();
      var value = array[i];
      if (value != null) {
        return value.equals(map.get(key));
      }
      return map.containsKey(key) && map.get(key) == null;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int h = 0;
    var fields = layout.fields();
    for(var i = 0; i < fields.size(); i++) {
      var key = fields.get(i).name();
      var value = array[i];
      h += key.hashCode() ^ Objects.hashCode(value);
    }
    return h;
  }

  @Override
  public Object get(Object key) {
    requireNonNull(key);
    return getOrDefault(key, null);
  }

  @Override
  public Object put(String key, Object value) {
    requireNonNull(key);
    var index = layout.fieldIndex(key);
    if (index == -1) {
      throw new IllegalStateException("invalid key " + key);
    }
    var old = array[index];
    array[index] = value;
    return old;
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("not supported");
  }

  @Override
  public void putAll(Map<? extends String, ?> map) {
    requireNonNull(map);
    map.forEach(this::put);
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("not supported");
  }

  @Override
  public Object getOrDefault(Object key, Object defaultValue) {
    requireNonNull(key);
    if (!(key instanceof String s)) {
      return defaultValue;
    }
    var index = layout.fieldIndex(s);
    if (index == -1) {
      return defaultValue;
    }
    var value = array[index];
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  @Override
  public boolean containsKey(Object key) {
    requireNonNull(key);
    if (!(key instanceof String s)) {
      return false;
    }
    return layout.fieldIndex(s) != -1;
  }

  @Override
  public boolean containsValue(Object value) {
    return Arrays.asList(array).contains(value);
  }

  @Override
  public void forEach(BiConsumer<? super String, ? super Object> action) {
    requireNonNull(action);
    var fields = layout.fields();
    for(var i = 0; i < fields.size(); i++) {
      var key = fields.get(i).name();
      var value = array[i];
      action.accept(key, value);
    }
  }

  @Override
  public List<Object> values() {
    return Arrays.asList(array);
  }

  @Override
  public Set<String> keySet() {
    var fields = layout.fields();
    return new AbstractSet<>() {
      @Override
      public int size() {
        return fields.size();
      }

      @Override
      public Iterator<String> iterator() {
        return new Iterator<>() {
          private int index;

          @Override
          public boolean hasNext() {
            return index < fields.size();
          }

          @Override
          public String next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return fields.get(index++).name();
          }
        };
      }
    };
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    var fields = layout.fields();
    return new AbstractSet<>() {
      @Override
      public int size() {
        return array.length;
      }

      @Override
      public Iterator<Entry<String, Object>> iterator() {
        return new Iterator<>() {
          private int index;

          @Override
          public boolean hasNext() {
            return index < array.length;
          }

          @Override
          public Entry<String, Object> next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return Map.entry(fields.get(index).name(), array[index++]);
          }
        };
      }
    };
  }
}