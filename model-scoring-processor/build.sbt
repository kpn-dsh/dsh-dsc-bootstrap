import com.typesafe.sbt.packager.docker.Cmd
// Plugins for building the project
enablePlugins(JavaServerAppPackaging, AshScriptPlugin)
cancelable in Global := true

// Name of the project
name := "model-scoring-processor"

// Spark works together with Scala 2.11
scalaVersion := "2.11.8"

// JVM opts for resources of the application inside the container
val jvmOpts = Seq(
  "-Xmx2g",
  "-Xms2g",
  "-XX:+UseConcMarkSweepGC"
)
javaOptions in Universal ++= jvmOpts.map(opt => s"-J$opt")
javaOptions in Test ++= jvmOpts

// Spark, Kafka dependencies are added to the project
libraryDependencies ++= Dependencies.spark ++ Dependencies.kafka
libraryDependencies ++= DependencyGroups.configuration
//protobuf dependencies + settings
libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.3.3"

// Spark requires Jackson 2.6.5 and this is overriden by some of the other dependencies so we need to put it back
dependencyOverrides ++= Set("com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.6.5",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.6.5",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.5",
  "org.json4s" % "json4s-jackson_2.11" % "3.2.11")

// protobuf-related settings
unmanagedResourceDirectories in Compile ++= (PB.protoSources in Compile).value
managedSourceDirectories.in(Compile) += target.value / "protobuf-generated"
PB.targets.in(Compile) := Seq(scalapb.gen(singleLineToString = true) -> (target.value / "protobuf-generated"))


/* Docker Build */
dockerRepository := Some("dataserviceshub-docker-dshdemo1.jfrog.io")
dockerBaseImage := DockerFile.baseImage
daemonUser in Docker := "dshdemo1"
daemonUserUid in Docker := Some("1024")
daemonGroup in Docker := "dshdemo1"
daemonGroupGid in Docker := Some("1024")
packageName in Docker := "model-scoring-processor"
bashScriptExtraDefines ++= IO.readLines(baseDirectory.value / "src/main/scripts" / "extra.sh")
mainClass in Compile := Some("com.klarrio.dsh.kpn.datascience.bootstrap.exercise.AnalyzerMain")
dockerCommands in Docker := DockerFile.rewrite(
  (dockerCommands in Docker).value,
  (daemonUser in Docker).value,
  (daemonUserUid in Docker).value,
  Seq.empty
)
dockerCommands ++= Seq(
  Cmd("RUN", "mkdir /opt/docker/models/"),
  Cmd("RUN", "mv pipeline_model_amt_callers_15min /opt/docker/models/"),
  Cmd("RUN", "mv pipeline_model_amt_queuers_15min /opt/docker/models/"),
  Cmd("RUN", "mv pipeline_model_wait_time_5min /opt/docker/models/"),
  Cmd("RUN", "mv pipeline_model_wait_time_15min /opt/docker/models/")
)

