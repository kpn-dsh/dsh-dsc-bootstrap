package com.klarrio.dsh.kpn.datascience.bootstrap.exercise

import java.util.concurrent.Future

import com.klarrio.zipper.messages.common.envelope.{DataEnvelope, KeyEnvelope}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils}
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.functions.col

/**
 * Starting point
 */

object AnalyzerMain {

  def main(args: Array[String]): Unit = {
    // Initialize Spark
    val (sparkSession, ssc) = initSpark()
    import sparkSession.implicits._

    // Load the pipeline models that were trained in batch
    val amtCallersModel = PipelineModel.load("/opt/docker/models/pipeline_model_amt_callers_15min")
    val amtQueuersModel = PipelineModel.load("/opt/docker/models/pipeline_model_amt_queuers_15min")
    val waitTime5mModel = PipelineModel.load("/opt/docker/models/pipeline_model_wait_time_5min")
    val waitTime15Model = PipelineModel.load("/opt/docker/models/pipeline_model_wait_time_15min")

    // Initialize Kafka consumer parameters
    val kafkaParams = initKafkaConsumerParams()

    // Initialize and broadcast the Kafka producer
    val kafkaProducer: Broadcast[SparkKafkaProducer] = {
      val kafkaProducerConfig = ConfigFetcher.kafkaProducerParams
      ssc.sparkContext.broadcast(SparkKafkaProducer(kafkaProducerConfig))
    }

    /**
     * 1. READ FROM KAFKA STREAM
     */



    /**
     * 2. PARSE THE JSON OBSERVATIONS
      * Use circe to parse the JSON list of messages in to the case classes
      * as in the following example: https://circe.github.io/circe/codec.html
      * return the pubTime and the parsed object of the case class
     */





    /**
     * 3. SCORE THE STREAM
      * 1. Do a left outer join of the queuersAndCallersStream with the averageHandlingTimeStream and the waitTimeStream
      *   It should be left outer join since the queuers and callers stream has data for each timestamp
      *   and we want a prediction for each timestamp.
      * 2. Merge the different metrics into an object of the Features case class
      *   If there is no data for average handling time or wait time, we impute them with zero for simplicity.
      *   In a real world scenario we would choose a better strategy for this.
     * 3. transform the rdd into a Dataframe and use the pipeline model to score the stream
     * 4. Select the prediction of the latest pub timestamp
      *5.  Use the value of "predictedLabel" as the value that is put in the appropriate JSON message for the prediction.
      * The appropriate JSON format is listed under here.
      * 6. Send that JSON message to Kafka
      *   topic: ConfigFetcher.predictedAmtCallers15mTopic
      *   key: "predicted-direction-callers"
      *   value: "[{\"predDirectionCallers15min\":\"" + predictedLabelValue + "\"}]"
      *
      *   topic: ConfigFetcher.predictedAmtQueuers15mTopic
      *   key: "predicted-amt-queuers"
      *   value: "[{\"predAmtQueuers15min\":\"" + predictedLabelValue + "\"}]"
      *
      *   topic: ConfigFetcher.predictedWait5mTopic
      *   key: "predicted-wait-time-5m"
      *   value: "[{\"predWaitTime5min\":\"" + predictedLabelValue + "\"}]"
      *
      *   topic: ConfigFetcher.predictedWait15mTopic
      *   key: "predicted-wait-time-15m"
      *   value: "[{\"predWaitTime15min\":\"" + predictedLabelValue + "\"}]"
     */





    /**
     * To start any Spark streaming application, you need to call start and awaitTermination on the streaming context
     */
    ssc.start()
    ssc.awaitTermination()
  }

  def initSpark(): (SparkSession, StreamingContext) = {
    val sparkSession: SparkSession = SparkSession.builder
      .appName("dsh-kpn-datascience-bootstrap-exercise")
      .master("local[*]")
      .config("spark.streaming.kafka.consumer.cache.enabled", "false")
      .getOrCreate

    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(ConfigFetcher.microBatchInterval))
    ssc.checkpoint("./checkpoint-dir")
    (sparkSession, ssc)
  }


  /**
    * Initializes the consumer parameters to read from Kafka.
    * It tells the consumer to deserialize the data into the structure of KeyEnvelopes and DataEnvelopes
    * And it contains some configurations for failures and restarts.
    * @return
    */
  def initKafkaConsumerParams(): Map[String, Object] = {
    ConfigFetcher.kafkaConsumerParams ++ Map[String, Object](
      "key.deserializer" -> classOf[KeyEnvelopeDeserializer],
      "value.deserializer" -> classOf[DataEnvelopeDeserializer],
      "enable.auto.commit" -> (false: java.lang.Boolean),
      "auto.offset.reset" -> "latest"
    )
  }
}

