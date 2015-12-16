import sbt._

object Dependencies {
  val scalaVersion = "2.11.7"
  val slf4jVersion = "1.7.12"
  val akkaVersion = "2.3.14"
  val sprayVersion = "1.3.3"

  val log = Seq("org.slf4j" % "slf4j-api" % slf4jVersion,
                "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
                "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
                "ch.qos.logback" % "logback-classic" % "1.1.2")

  val spray = Seq("io.spray"            %%  "spray-can"     % sprayVersion,
                  "io.spray"            %%  "spray-routing" % sprayVersion,
                  "io.spray"            %%  "spray-testkit" % sprayVersion  % "test",
                  "com.typesafe.akka"   %%  "akka-actor"    % akkaVersion,
                  "com.typesafe.akka"   %%  "akka-testkit"  % akkaVersion   % "test",
                  "org.specs2"          %%  "specs2-core"   % "2.3.13" % "test")

  val sprayJson = Seq("io.spray" %% "spray-json" % "1.3.1")

  val play = Seq("com.typesafe.play" %% "play" % "2.4.4")

  val test = Seq("org.scalatest" %% "scalatest" % "2.2.4" % Test,
                 "org.scalamock" %% "scalamock-scalatest-support" % "3.2.1" % Test)

  val basic = log ++ spray ++ sprayJson ++ play ++ test

  val testJar = Seq("com.wandoujia.n4c" %% "pedt" % "0.1")
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
