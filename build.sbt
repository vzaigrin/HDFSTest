ThisBuild / version := "1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "HDFSTest"
  )

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.2",
  "org.apache.hadoop" % "hadoop-client" % "3.2.1",
  "ch.qos.logback" % "logback-classic" % "1.2.11"
)
