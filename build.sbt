
name := "xml2json-converter"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "net.liftweb" %% "lift-json" % "3.3.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "2.1.0",
  "org.apache.kafka" % "kafka-streams-test-utils" % "2.1.0" % Test
)

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"  % "3.5.0"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)
dockerBaseImage := "openjdk:8-jre-alpine"

mainClass in Compile := Some("sns.lando.xml2json.converter.Xml2JsonConverterApp")