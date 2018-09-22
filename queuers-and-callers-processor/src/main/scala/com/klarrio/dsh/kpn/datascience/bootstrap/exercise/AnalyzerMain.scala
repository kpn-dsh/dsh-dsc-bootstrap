package com.klarrio.dsh.kpn.datascience.bootstrap.exercise

import java.sql.Timestamp
import java.text.SimpleDateFormat

import com.klarrio.zipper.messages.common.envelope.{DataEnvelope, KeyEnvelope}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils}
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Starting point of the application
  * You can keep the code of the application logic for simplicity of this first application.
  * This AnalyzerMain object contains a main function and is therefore the Main class.
  * This is the point from which the Scala application will start its execution.
  */
object AnalyzerMain {
  def main(args: Array[String]): Unit = {
    // Initialize Spark
    val (sparkSession, ssc) = initSpark()

    // Initialize Kafka consumer parameters
    val kafkaParams = initKafkaConsumerParams()

    /** Initialize and broadcast the Kafka producer
      * This makes sure that the Kafka producer is available in each executor and for each partition of the data
      */
    val kafkaProducer: Broadcast[SparkKafkaProducer] = {
      val kafkaProducerConfig = ConfigFetcher.kafkaProducerParams
      ssc.sparkContext.broadcast(SparkKafkaProducer(kafkaProducerConfig))
    }

    /**
     * 1. READ FROM KAFKA STREAM
      * If data is to be published on the platform it has to be in a certain structure.
      * Kafka messages always contain a key and a message. To able to publish them to the platform
      * these keys and messages need to be packaged in data structures called: KeyEnvelopes and DataEnvelopes.
      * Consequently, when we read data from the platform the data type of the incoming stream will be
      * [KeyEnvelope, DataEnvelope].
      * input topic = ConfigFetcher.inputCallcenterLogsTopic
      * data type of the stream: [KeyEnvelope, DataEnvelope]
      * use the kafkaParams variable for the kafka parameters
      * You will then get a stream of ConsumerRecords which contains some information about the Kafka data:
      * - topic
      * - key: of type KeyEnvelope
      * - value: of type DataEnvelope
      * - partition
      * - offset
      * - timestamp
      * Extract the JSON message from the data envelope by mapping the record:
      * (r: ConsumerRecord[KeyEnvelope, DataEnvelope] => record.value().getBinary.toStringUtf8)
      */
    val callcenterLogsStream = KafkaUtils
      .createDirectStream[KeyEnvelope, DataEnvelope](
        ssc,
        PreferConsistent,
        ConsumerStrategies.Subscribe[KeyEnvelope, DataEnvelope](
          Array(ConfigFetcher.inputCallcenterLogsTopic),
          kafkaParams
        )
      ).map { r: ConsumerRecord[KeyEnvelope, DataEnvelope] => r.value().getBinary.toStringUtf8 }

    callcenterLogsStream.print(30)

    /**
      * 2. PARSE THE STREAM
      * Parse the csv line by splitting it on comma and putting the elements in the case class.
      * The timestamps will be read as String and need to be parsed to datetime. Use the method implemented for this.
      * The order of the features is the same in the data as in the case class.
      */
    val dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    val simpleDateFormat = new SimpleDateFormat(dateFormat)

    val parsedLogsStream = callcenterLogsStream.transform(rdd =>
      rdd.map { str: String =>
        val spl = str.split(",")
        CallObservation(
          extractTimestamp(simpleDateFormat, spl(0)),
          spl(1),
          extractTimestamp(simpleDateFormat, spl(2)),
          extractTimestamp(simpleDateFormat, spl(3)),
          extractTimestamp(simpleDateFormat, spl(4)),
          extractTimestamp(simpleDateFormat, spl(5)),
          extractTimestamp(simpleDateFormat, spl(6)),
          extractTimestamp(simpleDateFormat, spl(7)),
          spl(8),
          spl(9),
          spl(10),
          spl(11)
        )
      })

    /**
     * 3. COMPUTE KPIs: QUEUERS AND CALLERS
      * Check if the RDD is empty
      * If the RDD is not empty compute the following:
      * 1. Amount of callers is the amount of records in the rdd
      * 2. For the amount of queuers: filter out the calls that are not being helped yet.
      *    Then compute the amount of observations in this filtered stream.
      * 3. Compute the ratio as a number between 0 and 1 that describes the percentage of the people that is being helped
      * 4.  Publish this average amount on Kafka
      *     Use the send function of the broadcasted kafkaProducer instance
      *     output topic: ConfigFetcher.outputKpiTopic
      *     kafka key: "queuers-and-callers"
      *     kafka value: "[{\"pubTime\":" + pubTime + ",\"queueSize\":" + amtOfQueuers + " ,\"callsAmt\":" + amtOfCallers + ",\"ratioServed\":" + ratio + "}]"
      *     The logic of the producer will package the key and the value in the appropriate KeyEnvelope and DataEnvelope
      *     data structures, serialize them into byte arrays, and publish them onto the Kafka topic.
    */
    parsedLogsStream.foreachRDD { rdd =>
      if (!rdd.isEmpty()) {
        val pubTime = rdd.map(_.pubTime.getTime).max()
        val amtOfCallers = rdd.count()

        val amtOfQueuers = rdd.filter { observation =>
          observation.pubTime.before(observation.dt_start) && observation.pubTime.after(observation.dt_offered_queue)
        }.count()

        val ratio = 1.0 - (amtOfQueuers.toDouble / amtOfCallers.toDouble)

        val record = "[{\"pubTime\":" + pubTime + ",\"queueSize\":" + amtOfQueuers + " ,\"callsAmt\":" + amtOfCallers + ",\"ratioServed\":" + ratio + "}]"

        kafkaProducer.value.send(ConfigFetcher.outputKpiTopic, "queuers-and-callers", record.toString)
      }
    }

    /**
      * To start any Spark streaming application, you need to call start and awaitTermination on the streaming context
      * You put this at the end of the application code. Spark will generate a JobGraph from the transformation and
      * operations that have been defined before this, it will optimize the operator chaining and execute the flow.
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

    /**
      * The streaming context is connected to the sparkSession and is configured with a certain microbatch interval.
      * The microbatch interval describes the interval at which new batches of data will be processed.
      */
    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(ConfigFetcher.microBatchInterval))

    /**
      * The checkpoint directory is the place where Spark will save its checkpoint data.
      * Checkpointing is used for failure recovery. It saves the state of the application at regular intervals
      * so that it can start from the latest saved state if a failure occurs.
      */
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


  /**
   * Parse timestamp of raw stream
   */
  def extractTimestamp(simpleDateFormat: SimpleDateFormat, obs: String): Timestamp = {
    new Timestamp(simpleDateFormat.parse(obs).getTime)
  }
}

