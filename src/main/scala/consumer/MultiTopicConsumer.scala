package consumer

import java.util.Properties
import java.util.Collection
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._


import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition

import scala.language.postfixOps

case class StartFromBeginning[A, B](consumer: KafkaConsumer[A, B]) extends ConsumerRebalanceListener {

  def onPartitionsRevoked(partitions: Collection[TopicPartition]) {
    consumer.commitSync()
  }

  def onPartitionsAssigned(partitions: Collection[TopicPartition]) {
    partitions.foreach { p ⇒ 
      println(s"Consuming from beggining for topic ${p.topic} using partition ${p.partition}")
      consumer.seekToBeginning(p)
    }
  }
}

case class StartFromLastCommit[A, B](consumer: KafkaConsumer[A, B]) extends ConsumerRebalanceListener {

  def onPartitionsRevoked(partitions: Collection[TopicPartition]) {
    consumer.commitSync()
  }

  def onPartitionsAssigned(partitions: Collection[TopicPartition]) {
    partitions.foreach { p ⇒ 
      val offsetMetadata = consumer.committed(p)
      if(offsetMetadata == null) {
        println(s"No offset commited. Consuming from beggining for topic ${p.topic} using partition ${p.partition}")
        consumer.seekToBeginning(p)
      } else  {
        println(s"Consuming from offset ${offsetMetadata.offset} for topic ${p.topic} using partition ${p.partition}")
        consumer.seek(p, offsetMetadata.offset)
      }
    }
  }
}


/**
 * Consumer using Kafka's new 0.9.0.0 consumer API
 */
class MultiTopicConsumer(
  topics: Seq[String], 
  groupId: String, 
  broker: String, 
  processor: MessageProcessor,
  startFromBeginning: Boolean = false,
  timeout: Long = 100)(implicit ec: ExecutionContext) {

  type RawConsumer = KafkaConsumer[Array[Byte], Array[Byte]]

  val props = new Properties()
  props.put("bootstrap.servers", broker)
  props.put("group.id", groupId)
  props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  props.put("enable.auto.commit", "false") //don't commit offset automatically

  val consumer = new RawConsumer(props)
  subscribe(topics, startFromBeginning)

  def readForever(): Unit = {
    while (true) {
      val records = consumer.poll(timeout)

      records.map { r ⇒ 
        Console.err.println(s"Processing ${r.topic} offset ${r.offset}")
        val f = processor.process(r.offset, r.topic, r.value)
        f onSuccess { 
          case result ⇒  { 
            Console.err.println(s"Processed ${r.topic} offset ${r.offset}")
          }
        }

        Await.result(f, 120 seconds)
        consumer.commitSync()

        Console.err.println(s"Offset ${r.offset} for ${r.topic} synced ")
      }
    }
  }

  def subscribe(topics: Seq[String], startFromBeginning: Boolean) {
    println(s"Subscribing to topics: ${topics}")
    if(startFromBeginning) {
      println(s"Consuming from beggining...")
      consumer.subscribe(topics.toList, StartFromBeginning(consumer))
    } else {
      consumer.subscribe(topics.toList, StartFromLastCommit(consumer))
    }
  }
}
