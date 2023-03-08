val scala3Version = "3.2.2"

ThisBuild / scalacOptions ++= Seq("-old-syntax", "-no-indent", "-source:future", "-Ykind-projector:underscores")
ThisBuild / scalaVersion := scala3Version
lazy val swingio = project
  .in(file("swingio"))
  .settings(
    name := "swing-io",
    version := "0.1.0-SNAPSHOT",


    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.4.8",
    libraryDependencies += "co.fs2" %% "fs2-core" % "3.6.1",
    libraryDependencies += "org.typelevel" %% "log4cats-core" % "2.5.0",
    libraryDependencies += "org.typelevel" %% "shapeless3-deriving" % "3.0.1",
    libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % "2.5.0" % Test,
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.6" % Test,
  )
lazy val simple = project
  .in(file("simple"))
  .dependsOn(swingio)
  .settings(
    name := "simple",
    assembly / assemblyJarName := "assembly.jar",
  )

lazy val root = 
  project.aggregate(swingio, simple)
