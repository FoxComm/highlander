package consumer

import java.util.Properties
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

import org.apache.kafka.clients.consumer._

/**
 * Consumer using Kafka's new 0.9.0.0 consumer API
 */
class MultiTopicConsumer(
  topics: Seq[String], 
  groupId: String, 
  broker: String, 
  processor: MessageProcessor,
  timeout: Long = 100) {

  val props = new Properties()
  props.put("bootstrap.servers", broker)
  props.put("group.id", groupId)
  props.put("enable.auto.commit", "false") //don't commit offset just yet.
  props.put("auto.offset.reset", "earliest")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")

  val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)
  consumer.subscribe(topics.toList)
  println(s"Subscribed to topics: ${consumer.subscription}")

  def readForever()(implicit ec: ExecutionContext): Unit = {
    while (true) {
      val records = consumer.poll(timeout)

      for (r ← records) {
        processor.process(r.offset, r.topic, r.value)
      }
    }
  }
}
