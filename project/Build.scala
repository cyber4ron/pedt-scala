import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object Dependencies {
  val scalaVersion = "2.11.7"
  val slf4jVersion = "1.7.12"
  val akkaVersion = "2.3.14"
  val sprayVersion = "1.3.3"

  val log = Seq("org.slf4j" % "slf4j-api" % slf4jVersion,
                "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
                "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
                "ch.qos.logback" % "logback-classic" % "1.1.2")

  val scalaReflect = Seq("org.scala-lang" % "scala-reflect" % scalaVersion)

  val spray = Seq("io.spray"            %%  "spray-can"     % sprayVersion,
                  "io.spray"            %%  "spray-routing" % sprayVersion,
                  "io.spray"            %%  "spray-testkit" % sprayVersion  % "test",
                  "com.typesafe.akka"   %%  "akka-actor"    % akkaVersion,
                  "com.typesafe.akka"   %%  "akka-testkit"  % akkaVersion   % "test",
                  "org.specs2"          %%  "specs2-core"   % "2.3.13" % "test")

  val sprayJson = Seq("io.spray" %% "spray-json" % "1.3.1")

  val play = Seq("com.typesafe.play" %% "play" % "2.4.4")

  val basic = log ++ spray ++ sprayJson ++ play
}

object Formatting {

  import com.typesafe.sbt.SbtScalariform
  import com.typesafe.sbt.SbtScalariform.ScalariformKeys

  val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(IndentSpaces, 2)
  }

  val settings = SbtScalariform.scalariformSettings ++
    Seq(ScalariformKeys.preferences in Compile := formattingPreferences,
        ScalariformKeys.preferences in Test := formattingPreferences)

}

object Build extends sbt.Build {

  lazy val basicSettings = Seq(organization := "com.wandoujia.n4c",
                               scalaVersion := "2.11.7",
                               scalacOptions ++= Seq("-unchecked", "-deprecation"),
                               resolvers ++= Seq("Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
                                                 "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"),
                               fork in run := true,
                               javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
                               credentials += Credentials(Path.userHome / ".ivy2" / ".wdj_credentials"),
                               checksums in update := Nil)
  assemblySettings

  lazy val extraSettings = Seq(assemblyOption in assembly ~= { _.copy(includeScala = false, includeDependency = false) })

  lazy val pedtScala = Project("pedt-scala", file(".")).
                       aggregate(pedt)

  lazy val pedt = Project("pedt", file("pedt")).
                  settings(basicSettings: _*).
                  settings(Formatting.settings: _*).
                  settings(libraryDependencies ++= Dependencies.basic).
                  settings(assemblySettings ++ extraSettings: _*)

  lazy val test = Project("test", file("test")).
                  dependsOn(pedt).
                  settings(basicSettings: _*).
                  settings(libraryDependencies ++= Dependencies.basic ++ Dependencies.scalaReflect).
                  settings(Formatting.settings: _*)


  lazy val sideTest = Project("side-test", file("side-test")).
                  dependsOn(pedt).
                  settings(basicSettings: _*).
                  settings(libraryDependencies ++= Dependencies.basic ++ Dependencies.scalaReflect).
                  settings(Formatting.settings: _*)
}
