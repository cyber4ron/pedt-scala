# pedt-scala
scala implementation of <a href="https://github.com/aimingoo/redpoll/blob/master/infra/specifications/PEDT.md">PEDT</a>, also see
<a href="https://github.com/aimingoo/redpoll">redpoll(NodeJS implementation)</a>, <a href="https://github.com/aimingoo/n4c">n4c</a>

## Prerequisites
<a href="http://www.scala-sbt.org/download.html">sbt</a> installed.

## Run test
```shell
$ git clone ...
$ cd pedt-scala
$ export JS_PEDT_SCRIPT=src/main/javascript/pedt.js
$ export JS_HELPER_SCRIPT=src/main/resources/js_helper.json
$ sbt test
```

## Publish to private repository
```shell
$ sbt pedt/publish
```

## Use as standalone worker
```shell
$ sbt pedt/assembly
$ java -jar .../pedt-assembly-0.1.jar
```

## Use as library
```scala
// first, publish the library to repository, then add it as dependency
libraryDependencies ++= "com.wandoujia.n4c" %% "pedt" % "0.1"
```

## How it works
![illustrate pedt-scala](https://github.com/cyber4ron/notes/blob/master/images/pedt-scala.png)
