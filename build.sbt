enablePlugins(JavaAppPackaging)

name := "synkr"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

maintainer := "Andreas Drobisch <andreas@drobisch.com>"

packageSummary := "synkr Debian Package"

packageDescription := "synkr synchronization app"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.41",
  "commons-codec" % "commons-codec" % "1.10",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe" % "config" % "1.3.1",
  "org.scalafx" %% "scalafx" % "8.0.102-R11"
)
