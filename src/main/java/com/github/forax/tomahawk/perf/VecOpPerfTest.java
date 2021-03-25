package com.github.forax.tomahawk.perf;

import com.github.forax.tomahawk.vec.U32Vec;
import com.github.forax.tomahawk.vec.Vec;
import com.github.forax.tomahawk.vec.VecOp;
import com.github.forax.tomahawk.vec.VecOpsPerf;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.lookup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(value = 1, jvmArgs = { "--add-modules", "jdk.incubator.foreign", "--add-modules", "jdk.incubator.vector" })
@State(Scope.Benchmark)
public class VecOpPerfTest {
  private static final VecOp VEC_OP = VecOp.of(lookup());

  private Path destPath, path1, path2;
  private Vec dest, v1, v2;

  private static final Vec DEST, V1, V2;
  private static final ByteBuffer DEST_BUFFER, V1_BUFFER, V2_BUFFER;
  static {
    Path destPath, path1, path2;
    try {
      destPath = Files.createTempFile("vec-op-benchmark", "");
      path1 = Files.createTempFile("vec-op-benchmark", "");
      path2 = Files.createTempFile("vec-op-benchmark", "");
      DEST = U32Vec.mapNew(null, path1, 100_000);
      V1 = U32Vec.mapNew(null, destPath, 100_000);
      IntStream.range(0, (int) V1.length()).forEach(i -> ((U32Vec) V1).setInt(i, i));
      V2 = U32Vec.mapNew(null, path2, 100_000);
      IntStream.range(0, (int) V2.length()).forEach(i -> ((U32Vec) V2).setInt(i, i));

      DEST_BUFFER = VecOpsPerf.asByteBuffer((U32Vec) DEST);
      V1_BUFFER = VecOpsPerf.asByteBuffer((U32Vec) V1);
      V2_BUFFER = VecOpsPerf.asByteBuffer((U32Vec) V2);

    } catch(IOException e) {
      throw new UncheckedIOException(e);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        V2.close();
        V1.close();
        DEST.close();
        Files.delete(path2);
        Files.delete(path1);
        Files.delete(destPath);
      } catch(IOException e) {
        throw new UncheckedIOException(e);
      }
    }));
  }

  @Setup
  public void setup() throws IOException {
    destPath = Files.createTempFile("vec-op-benchmark", "");
    path1 = Files.createTempFile("vec-op-benchmark", "");
    path2 = Files.createTempFile("vec-op-benchmark", "");
    dest = U32Vec.mapNew(null, destPath, 100_000);
    v1 = U32Vec.mapNew(null, path1, 100_000);
    IntStream.range(0, (int) v1.length()).forEach(i -> ((U32Vec) v1).setInt(i, i));
    v2 = U32Vec.mapNew(null, path2, 100_000);
    IntStream.range(0, (int) v2.length()).forEach(i -> ((U32Vec) v2).setInt(i, i));
  }

  @TearDown
  public void tearDown() throws IOException  {
    v2.close();
    v1.close();
    dest.close();
    Files.delete(path2);
    Files.delete(path1);
    Files.delete(destPath);
  }

  @Benchmark
  public Vec op_add_vecops() {
    VEC_OP.applyInt(dest, v1, v2, (a, b) -> a + b);
    return dest;
  }

  @Benchmark
  public Vec op_add_hand_crafted() {
    VecOpsPerf.handCraftedInt((U32Vec) dest, (U32Vec) v1, (U32Vec) v2, (a, b) -> a + b);
    return dest;
  }

  @Benchmark
  public Vec op_add_hand_crafted_hard_coded() {
    VecOpsPerf.handCraftedHardCodedInt(DEST_BUFFER, V1_BUFFER, V2_BUFFER, (a, b) -> a + b);
    return DEST;
  }

  /*@Benchmark
  public Vec op_add_vec_loop() {
    var implDest = (U32Vec) dest;
    var implV1 = (U32Vec) v1;
    var implV2 = (U32Vec) v2;
    for(var i = 0; i < dest.length(); i++) {
      implDest.setInt(i, implV1.getInt(i) + implV2.getInt(i));
    }
    return dest;
  }*/
}
