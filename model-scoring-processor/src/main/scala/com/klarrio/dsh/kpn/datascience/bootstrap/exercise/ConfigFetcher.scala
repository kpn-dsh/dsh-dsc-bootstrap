package com.klarrio.dsh.kpn.datascience.bootstrap.exercise

import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConverters._

/**
 * Fetches the configuration settings from the reference.conf file in src/main/resources
 */
object ConfigFetcher {
  def toMap(config: Config) = config.entrySet().asScala.map(e => e.getKey -> e.getValue.unwrapped()).toMap

  val conf = ConfigFactory.load()

  // Kafka parameters to read securely from Kafka
  val kafkaConsumerParams: Map[String, Object] = toMap(conf.getConfig("kafka.consumer"))
  // Kafka parameters to publish securely to Kafka
  val kafkaProducerParams: Map[String, Object] = toMap(conf.getConfig("kafka.producer"))

  val tenantForEnvelopes = "dshdemo1"
  val publisherForEnvelopes = "queuers-and-callers-processor"

  val microBatchInterval = conf.getInt("data.microbatch.interval")

  // Input topics
  val averageHandlingTimeTopic: String = conf.getString("data.kafka.topic.input.average-handling-time")
  val queuersAndCallersTopic: String = conf.getString("data.kafka.topic.input.queuers-and-callers")
  val queuersPerServiceTopic: String = conf.getString("data.kafka.topic.input.queuers-per-service")
  val waitTimeTopic: String = conf.getString("data.kafka.topic.input.wait-time")

  // Output topics
  val predictedAmtCallers15mTopic: String = conf.getString("data.kafka.topic.output.predicted-amt-callers-15m")
  val predictedAmtQueuers15mTopic: String = conf.getString("data.kafka.topic.output.predicted-amt-queuers-15m")
  val predictedWait15mTopic: String = conf.getString("data.kafka.topic.output.predicted-wait-15m")
  val predictedWait5mTopic: String = conf.getString("data.kafka.topic.output.predicted-wait-5m")
}
