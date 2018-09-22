package com.klarrio.dsh.kpn.datascience.bootstrap.exercise

import java.util.concurrent.Future

import com.google.protobuf.ByteString
import com.klarrio.zipper.messages.common.envelope.{DataEnvelope, KeyEnvelope}
import com.klarrio.zipper.messages.common.envelope.DataEnvelope.Payload
import com.klarrio.zipper.messages.common.envelope.KeyEnvelope.FixedHeader
import com.klarrio.zipper.messages.common.envelope.KeyEnvelope.FixedHeader.Identity
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}

import scala.collection.JavaConverters._

/**
  * Creates a Kafka producer and implements the publishing logic
  *
  */
class SparkKafkaProducer(createProducer: () => KafkaProducer[KeyEnvelope, DataEnvelope]) extends Serializable {

  /* This is the key idea that allows us to work around running into
     NotSerializableExceptions. */
  lazy val producer = createProducer()

  /**
    * Use this method to publish data onto Kafka topics
    * @param topic: topic onto which we will publish data
    * @param key: key of the message, as prescribed in the slides
    * @param value: JSON message in String format that contains the data for the dashboard in the right structure as prescribed by the slides.
    * @return
    */
  def send(topic: String, key: String, value: String): Future[RecordMetadata] =
    producer.send(new ProducerRecord[KeyEnvelope, DataEnvelope](
      topic,
      new KeyEnvelope(Some(FixedHeader(Some(Identity(tenant = ConfigFetcher.tenantForEnvelopes, publisher = ConfigFetcher.publisherForEnvelopes)), retained = false)), key),
      new DataEnvelope(payload = Payload.Binary(ByteString.copyFromUtf8(value)))
    ))

}

object SparkKafkaProducer {

  def apply(config: Map[String, Object]): SparkKafkaProducer = {
    val createProducerFunc = () => {
      val producer = new KafkaProducer[KeyEnvelope, DataEnvelope]((config ++ Map[String, Object](
        "key.serializer" -> classOf[KeyEnvelopeSerializer].getName,
        "value.serializer" -> classOf[DataEnvelopeSerializer].getName
      )).asJava)

      sys.addShutdownHook {
        // Ensure that, on executor JVM shutdown, the Kafka producer sends
        // any buffered messages to Kafka before shutting down.
        producer.close()
      }

      producer
    }
    new SparkKafkaProducer(createProducerFunc)
  }

}