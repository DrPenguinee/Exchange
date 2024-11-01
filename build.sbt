ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

val zioVersion = "2.1.11"
val monocleVersion = "3.2.0"

lazy val root = (project in file("."))
  .settings(
    name := "Exchange",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-prelude" % "1.0.0-RC31",
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
      "dev.zio" %% "zio-mock" % "1.0.0-RC12",
      "dev.optics" %% "monocle-core"  % monocleVersion,
    ),
  )
