scala-pickling
==============

**Scala Pickling** is an automatic serialization framework made for Scala. It's fast, boilerplate-free, and allows users to easily swap in/out different serialization formats (such as binary, or JSON), or even to provide your own custom serialization format.

Basic usage:

    import scala.pickling._
    import json._

    val pckl = List(1, 2, 3, 4).pickle
    val lst = pckl.unpickle[List[Int]]

## Quick Start

- make sure `scala-pickling.jar` is on your classpath
- use Scala 2.10.2

## Get Scala Pickling

Scala Pickling for Scala 2.10.2 is available on Sonatype! You can find Scala Pickling under groupID: `org.scala-lang` and artifactID: `scala-pickling_2.10`. The current version is 0.8.0-SNAPSHOT.

You can use Scala Pickling in your SBT project by simply adding the following dependency to your build file:

    libraryDependencies += "org.scala-lang" %% "scala-pickling" % "0.8.0-SNAPSHOT"

For a more illustrative example, see a [sample SBT project which uses Scala Pickling](https://github.com/xeno-by/sbt-example-pickling).

Or you can just [directly download the jar](https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=org.scala-lang&a=scala-pickling_2.10&v=0.8.0-SNAPSHOT&e=jar).

## What makes it different?

Scala Pickling...

- can be **Language-Neutral** if you want it to be. Changing the format of your serialized data is as easy as importing the correct implicit pickle format into scope. Out of the box, we currently support a fast Scala binary format, as well as JSON. Support is currently planned for other formats. Or, you can even roll your own custom pickle format!
- is **Automatic**. That is, without any boilerplate at all, one can instruct the framework to figure out how to serialize an arbitrary class instance. No need to register classes, no need to implement any methods.
- **Allows For Unanticipated Evolution**. That means that you don’t have to extend some marker trait in order to serialize a given Scala class. Just import the scala.pickling package and call pickle on the instance that you would like to serialize.
- gives you more **Typesafety**. No more errors from serialization/deserialization propagating to arbitrary points in your program. Unlike Java Serialization, errors either manifest themselves as compile-time errors, or runtime errors only at the point of unpickling.
- has **Robust Support For Object-Orientation**. While Scala Pickling is based on the elegant notion of pickler combinators from functional programming, it goes on to extend pickler combinators to be able to handle subtyping, a notion which does not exist in the purely functional paradigm. That means that if you pickle an instance of a subclass, and then try to unpickle as an instance of a superclass, you will still get back the original subclass which you initially pickled.
- **Happens At Compile-Time**. That means that it’s super-performant because serialization-related code is typically generated at compile-time and inlined where it is needed in your code. Scala Pickling is essentially fully-static, reflection is only used as a fallback when static (compile-time) generation fails.

<!-- This project aims to turn [a custom build of macro paradise](https://github.com/heathermiller/scala-pickling/tree/topic/scala-pickling) that we used in
[Object-Oriented Pickler Combinators and an Extensible Generation Framework](http://lampwww.epfl.ch/~hmiller/files/pickling.pdf)
into a standalone library that targets 2.10.x
(The library requires 2.10.2+).

Since we are making extensive use of quasiquotes, which cannot be added to 2.10.x due to binary and source compatibility
restrictions, building (but not using!) scala-pickling requires [a custom build of scalac](http://docs.scala-lang.org/overviews/macros/paradise.html#macro_paradise_for_210x).
Please note that scala-pickling can be used without any problems with vanilla scalac 2.10.2 -
custom scalac is only necessary to _build_ this library, not to compile against it or to link to it at runtime.

Known limitations:
  1. No support for `@pickleable`, since we cannot have macro annotations in 2.10.x.
  2. In the public API (and everywhere else), vanilla type tags are replaced with `scala.pickling.FastTypeTag/scala.pickling.fastTypeTag`.
  3. Picklers are generated directly at call sites, since we cannot have introduceTopLevel in 2.10.x.
  4. No runtime compilation, since it's not obvious how to package it without inducing a dependency on `scala-compiler.jar`.
 -->