/**
 * This package defines a hierarchy of {@link com.github.forax.tomahawk.vec.Vec},
 * a {@link com.github.forax.tomahawk.vec.Vec} being a column of values
 * (an array of values) each of them having the same size stored in memory.
 *
 * <p>
 * There are three kinds of {@link com.github.forax.tomahawk.vec.Vec},
 * <ul>
 *   <li>primitive Vecs that stores 8 bits, to 64 bits values ({@code element})
 *       as an array of values.
 *       <ul>
 *         <li>{@link com.github.forax.tomahawk.vec.U8Vec 8 bits Vec} of values
 *         <li>{@link com.github.forax.tomahawk.vec.U16Vec  16 bits Vec} of values
 *         <li>{@link com.github.forax.tomahawk.vec.U32Vec  32 bits Vec} of values
 *         <li>{@link com.github.forax.tomahawk.vec.U32Vec  32 bits Vec} of values
 *       </ul>
 *   <li>the {@link com.github.forax.tomahawk.vec.ListVec ListVec}
 *       conceptually represents a list of Vecs of the values of the same
 *       type using two Vecs, a Vec of values ({@code element}) that contains
 *       all the values and a vec of offsets ({@code offset}) that defines
 *       the list of values for an index as the values in between the offset
 *       at the index in {@code offset} and the following index.
 *   <li>the {@link com.github.forax.tomahawk.vec.StructVec StructVec} which
 *       represent an array of struct by storing a list of Vecs (a struct of arrays).
 * </ul>
 *
 * The values can be nullable, for that all the {@link com.github.forax.tomahawk.vec.Vec}
 * listed above can takes an optional {@link com.github.forax.tomahawk.vec.U1Vec}
 * which is an array of boolean that if false says that the value is {@code null}
 * independently of the value in the {code element}.
 * <br/>
 * The method {@link com.github.forax.tomahawk.vec.Vec#isNull(long)} checks if a value
 * at {@code index} is null or not, and {@link com.github.forax.tomahawk.vec.Vec#setNull(long)}
 * set the value at {@code index} to null.
 *
 * <p>
 * The values of the {@link com.github.forax.tomahawk.vec.Vec}s can be stored in
 * three kinds of memory
 * <ul>
 *   <li>a GC managed memory, by wrapping an existing Java array.
 *      <br/>
 *       By example, {@link com.github.forax.tomahawk.vec.U32Vec#wrap(int[])}
 *       to wrap a Java array of ints.
 *   <li>a mapped file in memory, by either using {@code map} to create a Vec
 *       on an existing file, {@code mapNew} to create a empty new mapped file with
 *       a length or {code builder} to create a new mapped file with some specific
 *       values
 *      <br/>
 *       By example,
 *       {@link com.github.forax.tomahawk.vec.U16Vec#map(com.github.forax.tomahawk.vec.U1Vec, java.nio.file.Path)}
 *       maps an existing file as a {@link com.github.forax.tomahawk.vec.U16Vec},
 *       {@link com.github.forax.tomahawk.vec.U64Vec#mapNew(com.github.forax.tomahawk.vec.U1Vec, java.nio.file.Path, long)}
 *       creates a new mapped file of 64 bits values and
 *       {@link com.github.forax.tomahawk.vec.U32Vec#builder(com.github.forax.tomahawk.vec.U1Vec.Builder, java.nio.file.Path, java.nio.file.OpenOption...)}
 *       creates a builder than can be used to store values inside a mapped file.
 *       <pre>
 *         var builder = U32Vec.builder(null, Path.of("a_file_name"));
 *         builder.appendInt(3)
 *                .appendFloat(42.5f);
 *         var vec = builder.toVec();
 *       </pre>
 *  <li>any other kind of memory allocated using a {@link jdk.incubator.foreign.MemorySegment}
 *      using {@code from}.
 *     <br/>
 *      By example, to create a {@link com.github.forax.tomahawk.vec.U8Vec 8 bits Vec} from
 *      a memory allocated by malloc
 *      <pre>
 *        var memorySegment = MemorySegment.allocateNative(1024);
 *        var u8vec = U8Vec.from(memorySegment);
 *        ...
 *      </pre>
 * </ul>
 *
 * Given that all Vecs are backed by a memory (mapped or not), this memory must be de-allocated
 * when the Vec is not used anymore using {@link com.github.forax.tomahawk.vec.Vec#close()}.
 * <br/>
 * The preferred way is to use a {@code try-with-resources}, by example
 * <pre>
 *   try (var vec = U16Vec.map(...) {
 *     ...
 *   }  // implicit call to close()
 * </pre>
 *
 * <p>
 * A {@link com.github.forax.tomahawk.vec.Vec} only defines a storage capability (8 bits, 16 bits, etc),
 * so it can load and store any Java type of the same size, by example a {@link com.github.forax.tomahawk.vec.U32Vec}
 * has both the method {@link com.github.forax.tomahawk.vec.U32Vec#getFloat(long)} and
 * {@link com.github.forax.tomahawk.vec.U32Vec#setInt(long, int)}.
 */
package com.github.forax.tomahawk.vec;