val scala3Version = "3.2.2"

ThisBuild / tlBaseVersion := "0.1"
ThisBuild / scalacOptions ++= Seq("-old-syntax", "-no-indent", "-source:future", "-Ykind-projector:underscores")
ThisBuild / scalaVersion := scala3Version
ThisBuild / startYear := Some(2023)
ThisBuild / organization := "io.github.thedrawingcoder-gamer"
ThisBuild / organizationName := "BulbyVR"
ThisBuild / organizationHomepage := Some(url("https://thedrawingcoder-gamer.github.io/"))
ThisBuild / tlCiReleaseBranches := Seq("master")
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/TheDrawingCoder-Gamer/Swing.IO"),
    "scm:git@github.com:TheDrawingCoder-Gamer/Swing.IO.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "bulbyvr",
    name = "Ben Harless",
    email = "benharless820@gmail.com",
    url = url("https://thedrawingcoder-gamer.github.io/")
  )
)
ThisBuild / tlSonatypeUseLegacyHost := false
ThisBuild / description := "Swing in the IO Monad"
ThisBuild / licenses := List(
  "Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / homepage := Some(url("https://github.com/TheDrawingCoder-Gamer/Swing.IO"))

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true
ThisBuild / publishTo := {
  val sonatype = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at sonatype + "content/repositories/snapshots")
  else
    Some("releases" at sonatype + "service/local/staging/deploy/maven2")
}

lazy val swingio = project
  .in(file("swingio"))
  .settings(
    name := "swing-io",
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.8",
    libraryDependencies += "co.fs2" %% "fs2-core" % "3.6.1",
    libraryDependencies += "org.typelevel" %% "log4cats-core" % "2.5.0",
    libraryDependencies += "org.typelevel" %% "shapeless3-deriving" % "3.0.1",
    libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % "2.5.0" % Test,
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.6" % Test,
  )
lazy val simple = project
  .enablePlugins(NoPublishPlugin)
  .in(file("simple"))
  .dependsOn(swingio)
  .settings(
    name := "simple",
    assembly / assemblyJarName := "assembly.jar",
  )

lazy val root = 
  project
  .enablePlugins(NoPublishPlugin)
  .aggregate(swingio, simple)


