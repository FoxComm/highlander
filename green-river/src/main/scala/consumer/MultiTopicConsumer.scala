package consumer

import java.util.Properties
import java.util.Collection
import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

import consumer.aliases._
import org.apache.kafka.clients.consumer._
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.KafkaException

import scala.language.postfixOps

private object Sync {
  def commit[A, B](consumer: KafkaConsumer[A, B]): Unit = {
    try {
      consumer.commitSync()
    } catch {
      case e: CommitFailedException ⇒ Console.err.println(s"Failed to commit: $e")
      case e: KafkaException        ⇒ Console.err.println(s"Unexpectedly to commit: $e")
    }
  }

  def commit[A, B](
      consumer: KafkaConsumer[A, B], offsets: Map[TopicPartition, OffsetAndMetadata]): Unit = {
    try {
      consumer.commitSync(offsets)
    } catch {
      case e: CommitFailedException ⇒ Console.err.println(s"Failed to commit: $e")
      case e: KafkaException        ⇒ Console.err.println(s"Unexpectedly to commit: $e")
    }
  }
}

private case class StartFromBeginning[A, B](consumer: KafkaConsumer[A, B])
    extends ConsumerRebalanceListener {

  def onPartitionsRevoked(partitions: Collection[TopicPartition]): Unit = {
    Sync.commit(consumer)
  }

  def onPartitionsAssigned(partitions: Collection[TopicPartition]): Unit = {
    partitions.foreach { p ⇒
      Console.out.println(
          s"Consuming from beggining for topic ${p.topic} using partition ${p.partition}")
      consumer.seekToBeginning(p)
    }
  }
}

private case class StartFromLastCommit[A, B](consumer: KafkaConsumer[A, B])
    extends ConsumerRebalanceListener {

  def onPartitionsRevoked(partitions: Collection[TopicPartition]): Unit = {
    Sync.commit(consumer)
  }

  def onPartitionsAssigned(partitions: Collection[TopicPartition]): Unit = {
    partitions.foreach { p ⇒
      val offsetMetadata = consumer.committed(p)
      if (offsetMetadata == null) {
        Console.out.println(
            s"No offset commited. Consuming from beggining for topic ${p.topic} using partition ${p.partition}")
        consumer.seekToBeginning(p)
      } else {
        Console.out.println(
            s"Consuming from offset ${offsetMetadata.offset} for topic ${p.topic} using partition ${p.partition}")
        consumer.seek(p, offsetMetadata.offset)
      }
    }
  }
}

/**
  * Consumer using Kafka's new 0.9.0.0 consumer API
  */
class MultiTopicConsumer(topics: Seq[String],
                         groupId: String,
                         broker: String,
                         processor: MessageProcessor,
                         startFromBeginning: Boolean = false,
                         timeout: Long = 100)(implicit ec: EC) {

  type RawConsumer = KafkaConsumer[Array[Byte], Array[Byte]]

  val props = new Properties()
  props.put("bootstrap.servers", broker)
  props.put("group.id", groupId)
  props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  props.put("enable.auto.commit", "false") //don't commit offset automatically

  val consumer = new RawConsumer(props)
  subscribe(topics, startFromBeginning)

  def readForever(): Unit = {
    while (true) {
      val records = consumer.poll(timeout)

      val lastOffset = records.foldLeft(Map.empty[TopicPartition, OffsetAndMetadata]) {
        case (offsets, r) ⇒
          Console.err.println(s"\nProcessing ${r.topic} offset ${r.offset}")

          val result = Try {
            val f = processor.process(r.offset, r.topic, r.key, r.value)
            Await.result(f, 120 seconds)
          }
          result match {
            case Success(_) ⇒
              Console.err.println(s"Processed: ${r.topic} offset: ${r.offset}")
              val tp  = new TopicPartition(r.topic, r.partition)
              val off = new OffsetAndMetadata(r.offset + 1)
              offsets + (tp → off)
            case Failure(e) ⇒
              Console.err.println(s"Not processed: ${r.topic} offset: ${r.offset}")
              Console.err.println(s"Failure during processing ${r.topic} offset ${r.offset}: $e")
              offsets
          }
      }

      if (lastOffset.nonEmpty) {
        Sync.commit(consumer, lastOffset)
        Console.err.println(s"Synced offset: ${lastOffset}\n")
      }
    }
  }

  def subscribe(topics: Seq[String], startFromBeginning: Boolean): Unit = {
    Console.out.println(s"Subscribing to topics: $topics")
    if (startFromBeginning) {
      Console.out.println(s"Consuming from beggining...")
      consumer.subscribe(topics.toList, StartFromBeginning(consumer))
    } else {
      consumer.subscribe(topics.toList, StartFromLastCommit(consumer))
    }
  }
}
