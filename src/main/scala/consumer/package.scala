package consumer

import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.stream.Materializer

import com.sksamuel.elastic4s.source.DocumentSource
import scala.concurrent.Future

/**
  * Aliases
  */
object aliases {
  type AM = Materializer
  type AS = ActorSystem
  type CP = ConnectionPoolSettings
  type EC = scala.concurrent.ExecutionContext
}

/**
 * A processor that expects json.
 */
trait JsonProcessor {
  def process(offset: Long, topic: String, json: String) : Future[Unit]
}

/**
 * Simple interface to abstract out how we get messages from kafka
 */
trait MessageProcessor {
  def process(offset: Long, topic: String, message: Array[Byte]) : Future[Unit]
}

final case class PassthroughSource(json: String) extends DocumentSource
