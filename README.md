# pedt-scala
implementation of PEDT

## run test

    $ git clone ...
    $ cd pedt-scala
    $ export JS_PEDT_SCRIPT=src/main/javascript/pedt.js
    $ export JS_HELPER_SCRIPT=src/main/resources/js_helper.json
    $ sbt test
# publish to private repository

    $ sbt pedt/publish

## use as standalone worker
```shell
sbt pedt/assembly
java -jar pedt-assembly-0.1.jar
```
## use as library
```scala
// first, publish the library to repository, then add it as dependency
libraryDependencies ++= "com.wandoujia.n4c" %% "pedt" % "0.1"
```

## How it work    
![illustrate pedt-scala](https://github.com/cyber4ron/notes/blob/master/images/pedt-scala.png)

