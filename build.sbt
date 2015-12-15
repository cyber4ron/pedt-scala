import sbtassembly.Plugin.AssemblyKeys._

lazy val basicSettings = Seq(organization := "com.wandoujia.n4c",
                             scalaVersion := "2.11.7",
                             scalacOptions ++= Seq("-unchecked", "-deprecation"),
                             resolvers ++= Seq("Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
                                               "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"),
                             fork in run := true,
                             fork in Test := true,
                             javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
                             javacOptions in Test ++= Seq(),
                             unmanagedResourceDirectories in Compile += baseDirectory.value / "src/main/javascript",
                             credentials += Credentials(Path.userHome / ".ivy2/.wdj_credentials"),
                             checksums in update := Nil)

assemblySettings

lazy val extraSettings = Seq(assemblyOption in assembly ~= { _.copy(includeScala = false, includeDependency = false) })

lazy val pedtScala = (project in file(".")).
                     aggregate(pedt)

lazy val pedt = (project in file("pedt")).
                settings(Formatting.settings: _*).
                settings(basicSettings: _*).
                settings(libraryDependencies ++= Dependencies.basic).
                settings(assemblySettings ++ extraSettings: _*)
