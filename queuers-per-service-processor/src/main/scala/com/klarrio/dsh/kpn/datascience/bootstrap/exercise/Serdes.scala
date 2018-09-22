package com.klarrio.dsh.kpn.datascience.bootstrap.exercise

import java.util.{Map => JMap}

import com.klarrio.zipper.messages.common.envelope.{DataEnvelope, KeyEnvelope}
import org.apache.kafka.common.serialization._

import scala.util.Try


/**
  * If data is to be published on the platform it has to be in a certain structure.
  * Kafka messages always contain a key and a message. To able to publish them to the platform
  * these keys and messages need to be packaged in data structures called: KeyEnvelopes and DataEnvelopes.
  * These de-/serializers are the tools we will use to transform these KeyEnvelopes and DataEnvelopes into byte arrays.
  */

/**
  * Deserializes the key of Kafka messages coming from the platform
  */
class KeyEnvelopeDeserializer extends Deserializer[KeyEnvelope] {
  override def configure(configs: JMap[String, _], isKey: Boolean) {}
  override def close() {}
  override def deserialize(topic: String, data: Array[Byte]): KeyEnvelope =
    Try(KeyEnvelope.parseFrom(data)).getOrElse(KeyEnvelope())
}

/**
  * Deserializes the values of Kafka messages coming from the platform
  */
class DataEnvelopeDeserializer extends Deserializer[DataEnvelope] {
  override def configure(configs: JMap[String, _], isData: Boolean) {}
  override def close() {}
  override def deserialize(topic: String, data: Array[Byte]): DataEnvelope =
    Try(DataEnvelope.parseFrom(data)).getOrElse(DataEnvelope())
}


/**
  * Serializes the key of Kafka messages coming from the platform
  */
class KeyEnvelopeSerializer extends Serializer[KeyEnvelope] {
  override def configure(configs: JMap[String, _], isData: Boolean) {}
  override def close() {}
  override def serialize(topic: String, data: KeyEnvelope): Array[Byte] = data.toByteArray
}

/**
  * Serializes the values of Kafka messages coming from the platform
  */
class DataEnvelopeSerializer extends Serializer[DataEnvelope] {
  override def configure(configs: JMap[String, _], isData: Boolean) {}
  override def close() {}
  override def serialize(topic: String, data: DataEnvelope): Array[Byte] = data.toByteArray
}
