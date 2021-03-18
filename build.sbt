name := "sns-incoming-operator-messages-converter"

version := "0.1.1"

scalaVersion := "2.13.1"

resolvers += "mvnrepository" at "http://central.maven.org/maven2/"
resolvers += "Maven Repository" at "https://mvnrepository.com/artifact/"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
//  "net.liftweb" %% "lift-json" % "3.3.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "2.7.0",
  "org.apache.kafka" % "kafka-streams-test-utils" % "2.7.0" % Test,
//  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "org.json4s" %% "json4s-native" % "3.6.6",
  "org.json4s" %% "json4s-xml" % "3.6.6",
  "io.zipkin.brave" % "brave-instrumentation-kafka-clients" % "5.13.3",
  "io.zipkin.brave" % "brave-instrumentation-kafka-streams" % "5.13.3",
  "io.zipkin.reporter2" % "zipkin-sender-kafka11" % "2.8.1"
)
libraryDependencies += "com.dimafeng" %% "testcontainers-scala" % "0.26.0" % "test"
libraryDependencies += "org.testcontainers" % "kafka" % "1.11.1" % Test
libraryDependencies += "org.testcontainers" % "junit-jupiter" % "1.11.1" % Test
libraryDependencies += "net.javacrumbs.json-unit" % "json-unit" % "2.6.1" % Test

enablePlugins(GraalVMNativeImagePlugin)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
enablePlugins(AshScriptPlugin)

dockerBaseImage := "openjdk:8-jre"
//dockerBaseImage := "openjdk:8-jre-alpine"

mainClass in Compile := Some("converter.Xml2JsonConverterApp")
//http://central.maven.org/maven2/io/zipkin/reporter2/zipkin-sender-kafka11/2.8.1/zipkin-sender-kafka11-2.8.1.jar
//http://central.maven.org/maven2/io/zipkin/reporter2/zipkin-sender-kafka11_2.12/2.8.1/zipkin-sender-kafka11_2.12-2.8.1.pom