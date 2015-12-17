package consumer

import com.sksamuel.elastic4s.source.DocumentSource
import scala.concurrent.Future

/**
 * A processor that expects json.
 */
trait JsonProcessor {
  def beforeAction()
  def process(offset: Long, topic: String, json: String) : Future[Unit]
}

/**
 * Simple interface to abstract out how we get messages from kafka
 */
trait MessageProcessor {
  def process(offset: Long, topic: String, message: Array[Byte]) : Future[Unit]
}

final case class PassthroughSource(json: String) extends DocumentSource
