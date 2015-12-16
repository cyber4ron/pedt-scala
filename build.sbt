

lazy val basicSettings = Seq(organization := "com.wandoujia.n4c",
                             scalaVersion := "2.11.7",
                             scalacOptions ++= Seq("-unchecked", "-deprecation"),
                             resolvers ++= Seq("Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
                                               "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
                                               Resolver.mavenLocal),
                             fork in run := true,
                             fork in Test := true,
                             javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
                             javacOptions in Test ++= Seq(),
                             unmanagedResourceDirectories in Compile += baseDirectory.value / "src/main/javascript",
                             checksums in update := Nil)

lazy val publishSettings = Seq(publishTo := {
                                  val nexus = "http://mvn.corp.wandoujia.com/nexus/content/repositories/"
                                  if (version.value.trim.endsWith("SNAPSHOT"))
                                    Some("snapshots" at nexus + "wdj_snapshots")
                                  else
                                    Some("releases" at nexus + "wdj_repo_hosted")
                                },
                               credentials += Credentials(Path.userHome / ".ivy2" / ".wdj_credentials"),
                               // publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
                               publishMavenStyle := true,
                               publishArtifact in Test := false,
                               pomIncludeRepository := {
                                 (repo: MavenRepository) => false
                               })

assemblySettings

// lazy val extraSettings = Seq(assemblyOption in assembly ~= { _.copy(includeScala = false, includeDependency = false) })

lazy val root = Project("root", file("."))
                .aggregate(pedt)

lazy val pedt = Project("pedt", file("pedt"))
                .settings(libraryDependencies ++= Dependencies.basic)
                .settings(Formatting.settings: _*)
                .settings(basicSettings: _*)
                .settings(assemblySettings/* ++ extraSettings*/: _*)
                .settings(publishSettings: _*)
