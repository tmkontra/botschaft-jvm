import Dependencies._

ThisBuild / scalaVersion     := "2.13.4"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "botschaftj",
    libraryDependencies ++= dependencies
  )


val circeVersion = "0.12.3"

val Http4sVersion = "0.21.4"

lazy val dependencies = Seq(
  scalaTest % Test,
  "dev.zio" %% "zio" % "1.0.4-2",
  "org.typelevel" %% "cats-core" % "2.1.1",
  "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC11",
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion
) ++ (Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion))

