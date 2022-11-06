ThisBuild / version := "1.0"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "HDFSTest"
  )

libraryDependencies ++= Seq(
  "com.typesafe"      % "config"          % "1.4.2",
  "org.apache.hadoop" % "hadoop-client"   % "3.2.1",
  "ch.qos.logback"    % "logback-classic" % "1.3.0"
)

assembly / assemblyMergeStrategy := {
  case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
  case m if m.toLowerCase.endsWith("meta-inf")    => MergeStrategy.discard
  case "module-info.class"                        => MergeStrategy.first
  case _                                          => MergeStrategy.first
}
