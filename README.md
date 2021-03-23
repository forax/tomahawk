# Tomahawk
A better Java API for an Apache Arrow copycat

With the JDK 16, there are two API in incubation,
- `jdk.incubator.foreign` to manage foreign memory (aka non GC managed memory aka native memory)
- `jdk.incubator.vector` to generate AVX (Intel / AMD) and Neon (ARM) vector instructions

As a kind of tech preview, the idea is that those APIs are enough to re-implement the low-level part
of Apache Arrow which currently uses a C library for that.

---
WARNING !! this is NOT a drop in replacement of Apache Arrow, DO NOT USE IT for anything serious,
This is just an implementation comparable to `org.apache.arrow.vector` with, I think, a better API.
---

This API is composed of two packages
- `com.github.forax.tomahawk.vec` that contains implementation for columnar data aka `Vec`
   A `Vec` is a fixed sized mutable array of values 
   - there are primitive `Vec`, `U8Vec` for 8 bits values, `U16Vec` for 16 bits values, `U32Vec`
     for 32 bis values and `U64Vec` for 64 bits values.
   - there is a `ListVec`, conceptually an array of array of values, stored into two Vecs,
     an array of offsets and an array of values (the offsets indicates the start and the end of each lists).
   - there is a `StructVec`, conceptually an array of struct but stored as a struct of array
     (a struct of `Vec`)
     
   Unlike the vectors of Apache Arrow that can grow, a `Vec` can not grow but each `Vec` has its
   corresponding builder that can append values into a file that will be mapped as a `Vec`.
   A `Vec` only specify the storage (8 bits to 64 bits) which means that the `U32Vec` has method
   to load and store both `int` and `float`.
  
   This package also comes with an engine `VecOp` (not finished) that can perform basic computations
   on Vecs using the vector instructions.
  
   Here is the only bench that currently works, on my laptop, adding two columns is faster
   with the vector API (it should be 8 times faster theoretically but there are some bounds checks
   still there)
   ```
   Benchmark                            Mode  Cnt    Score   Error  Units
   VecOpPerfTest.op_add                 avgt    5   36.985 ± 0.234  us/op
   VecOpPerfTest.op_add_handcoded       avgt    5  188.107 ± 0.651  us/op
   ```
  
   I may be able to do better, I have some ideas on how to do partial evaluation of the memory zone
   of the `Vec` (once mapped in memory, the address do not change until it is deallocated)
   once I will have more time.

- `com.github.forax.tomahawk.schema` that defines a `Table`, a database table with a name,
  a directory to manage all mapped files of each `Vec` and a `Layout` that specifies column name
  and exact type.
  
  A Layout is homoiconic (and LISPy) in the sense that the definition in Java or in text are identical.
  By example, to define a table with a name, an age, a user itself composed of an id and an admin flag
  and a list of addresses
  ```java
  struct(true,
    field("name", string(false)),
    field("age", byte8(true)),
    field("user", struct(false,
      field("id", int32(false)),
      field("admin", u1(false))
    )),
    field("addresses", list(true, string(false)))
  )
  ```
  This definition as text can be fed to the method `Layout.parse()` or directly described in Java,
  by adding the static import `import static com.github.forax.tomahawk.schema.Layout.*`
  (the boolean after the type indicate if the type is nullable or not).
  
  By separating the notion of `Vec` and the notion of schema/`Layout`, one can easily rename or
  rearrange columns without changing the data, or better (or worst) share columns between
  different tables.
  
  Apart creating a table by hand, a table can be created from a CSV file or a JSON file,
  with `CSV.fetch()` and `JSON.fetch()` respectively.
  