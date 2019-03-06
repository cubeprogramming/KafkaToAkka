name := "kafka-to-akka"

version := "1.0"

scalaVersion := "2.12.7"

lazy val akkaVersion = "2.5.21"
lazy val betterFilesVersion = "3.7.0"
lazy val sprayVersion = "1.3.5"
lazy val kafka_streams_scala_version = "0.2.1"
lazy val kafka_client_version = "2.1.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "ch.qos.logback"      % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-RC2",
  "com.lightbend.akka"  %% "akka-stream-alpakka-file"  % "1.0-M2",
  "com.typesafe.play" %% "play" % "2.7.0", 
  /*"org.typelevel" %% "cats" % "0.4.0", */
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.github.pathikrit" %% "better-files" % betterFilesVersion,
  "io.spray" %%  "spray-json" % sprayVersion,
  "com.lightbend" %% "kafka-streams-scala" % kafka_streams_scala_version,
  "org.apache.kafka" % "kafka-clients" % kafka_client_version
)
