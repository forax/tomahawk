package com.github.forax.tomahawk.vec;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.vector.IntVector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class VecOpTest {
  public static Stream<Arguments> provideIntVecs() {
    Function<Path, Vec> byte8Factory = path -> {
      try {
        var vec = U8Vec.mapNew(null, path, 64);
        IntStream.range(0, 64).forEach(i -> vec.setByte(i, (byte) i));
        return vec;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
    Function<Path, Vec> short16Factory = path -> {
      try {
        var vec = U16Vec.mapNew(null, path, 64);
        IntStream.range(0, 64).forEach(i -> vec.setShort(i, (short) i));
        return vec;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
    Function<Path, Vec> int32Factory = path -> {
      try {
        var vec = U32Vec.mapNew(null, path, 64);
        IntStream.range(0, 64).forEach(i -> vec.setInt(i, i));
        return vec;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };

    var factories = List.of(byte8Factory, short16Factory, int32Factory);
    return factories.stream()
        .flatMap(factory1 -> factories.stream().map(factory2 -> Arguments.of(factory1, factory2)))
        .flatMap(arguments -> Stream.of(arguments, arguments));  // test each twice
  }


  private static final VecOp VEC_OP = VecOp.of(MethodHandles.lookup());

  @ParameterizedTest
  @MethodSource("provideIntVecs")
  public void applyIntToInt32(Function<Path, Vec> factory1, Function<Path, Vec> factory2) throws IOException {
    var pathDest = Files.createTempFile("vDest", "");
    var path1 = Files.createTempFile("v1", "");
    var path2 = Files.createTempFile("v2", "");
    Closeable andClean = () -> {
      Files.delete(pathDest);
      Files.delete(path1);
      Files.delete(path2);
    };
    try(andClean)  {
      try(var vDest = U32Vec.mapNew(null, pathDest, 64);
          var v1 = factory1.apply(path1);
          var v2 = factory2.apply(path2)) {

        //System.err.println("v1 " + v1.getClass() + " v2 " + v2.getClass());
        VEC_OP.applyInt(vDest, v1, v2, (a, b) -> a + b);

        for(var i = 0; i < vDest.length(); i++) {
          assertEquals(i * 2, vDest.getInt(i));
        }
      }
    }
  }
}