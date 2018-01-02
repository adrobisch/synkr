enablePlugins(JavaAppPackaging)

name := "synkr"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.4"

maintainer := "Andreas Drobisch <andreas@drobisch.com>"

packageSummary := "synkr Debian Package"

packageDescription := "synkr synchronization app"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.41",
  "commons-codec" % "commons-codec" % "1.10",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.dorkbox" % "SystemTray" % "3.12",
  "org.scalafx" %% "scalafx" % "8.0.102-R11",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test,
  "org.backuity.clist" %% "clist-core"   % "3.2.2",
  "org.backuity.clist" %% "clist-macros" % "3.2.2" % "provided"
)

val circeVersion = "0.9.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
