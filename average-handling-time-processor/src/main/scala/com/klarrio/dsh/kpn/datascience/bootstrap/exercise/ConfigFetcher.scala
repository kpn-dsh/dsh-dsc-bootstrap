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

  val inputCallcenterLogsTopic = conf.getString("data.kafka.input.callcenter.logs.topic")
  val inputOutagesLogsTopic = conf.getString("data.kafka.input.outages.logs.topic")
  val outputKpiTopic = conf.getString("data.kafka.output.topic")
}
