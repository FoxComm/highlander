package consumer

import akka.actor.ActorSystem
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.Materializer

import com.sksamuel.elastic4s.source.DocumentSource
import scala.concurrent.Future
import scalacache.ScalaCache
import scalacache.serialization.InMemoryRepr
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.json4s.JValue

/**
  * Aliases
  */
object aliases {
  type AM       = Materializer
  type AS       = ActorSystem
  type CP       = ConnectionPoolSettings
  type EC       = scala.concurrent.ExecutionContext
  type SC       = ScalaCache[InMemoryRepr]
  type Json     = JValue
  type SRClient = CachedSchemaRegistryClient
}

/**
  * A processor that expects json.
  */
trait JsonProcessor {
  def process(offset: Long, topic: String, key: String, json: String): Future[Unit]
}

/**
  * Simple interface to abstract out how we get messages from kafka
  */
trait MessageProcessor {
  def process(offset: Long, topic: String, key: Array[Byte], message: Array[Byte]): Future[Unit]
}

final case class PassthroughSource(json: String) extends DocumentSource
