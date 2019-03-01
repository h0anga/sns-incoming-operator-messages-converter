name := "sns-incoming-operator-messages-converter"

version := "0.1.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "net.liftweb" %% "lift-json" % "3.3.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "2.1.0",
  "org.apache.kafka" % "kafka-streams-test-utils" % "2.1.0" % Test,
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.5.0",
  "org.json4s" %% "json4s-native" % "3.6.3",
  "org.json4s" %% "json4s-xml" % "3.6.5"

)

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

dockerBaseImage := "openjdk:8-jre"
//dockerBaseImage := "openjdk:8-jre-alpine"

mainClass in Compile := Some("converter.Xml2JsonConverterApp")
