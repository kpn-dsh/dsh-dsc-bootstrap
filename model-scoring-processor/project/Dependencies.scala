import sbt._

object Version {
  final val spark = "2.3.1"

  final val Kafka = "0.10.2.1"
  final val TypesafeConfig = "1.3.3"
  final val ScalaLogging = "3.9.0"
  final val Logback = "1.2.3"
  final val Slf4j = "1.7.25"
  final val ScalaTest = "3.0.5"
  final val DropwizardMetrics = "3.2.6"
  final val jackson = "2.8.7"
  final val circe = "0.9.3"
}


object Dependencies {
  val spark = Seq(
    "org.apache.spark" % s"spark-core_2.11" % Version.spark,
    "org.apache.spark" % s"spark-sql_2.11" % Version.spark,
    "org.apache.spark" % s"spark-mllib_2.11" % Version.spark,
    "org.apache.spark" % s"spark-streaming_2.11" % Version.spark,
    "org.apache.spark" % s"spark-streaming-kafka-0-10_2.11" % Version.spark
  )

  val kafka = Seq(
    "org.apache.kafka" % s"kafka_2.11" % Version.Kafka,
    // circe for json parsing
    "io.circe" %% "circe-core" % Version.circe,
    "io.circe" %% "circe-generic" % Version.circe,
    "io.circe" %% "circe-parser" % Version.circe
  ).map(_.exclude("log4j", "log4j")
    .exclude("org.slf4j", "slf4j-log4j12"))
}

object Library {
  val typesafeConfig: ModuleID = "com.typesafe" % "config" % Version.TypesafeConfig
  val scalaLogging: ModuleID = "com.typesafe.scala-logging" %% "scala-logging" % Version.ScalaLogging
  val logback: ModuleID = "ch.qos.logback" % "logback-classic" % Version.Logback
  val jacksonCore: ModuleID = "com.fasterxml.jackson.core" % "jackson-core" % Version.jackson
  val jacksonDatabind: ModuleID = "com.fasterxml.jackson.core" % "jackson-databind" % Version.jackson
  val scalaTest: ModuleID = "org.scalatest" %% "scalatest" % Version.ScalaTest
}

object DependencyGroups {
  val configuration = Seq(Library.typesafeConfig)
  val logging = Seq(Library.scalaLogging, Library.logback)
  val jackson = Seq(Library.jacksonCore, Library.jacksonDatabind)
  val testing = Seq(Library.scalaTest % "test")
}
