package consumer

import scala.concurrent.ExecutionContext

import com.sksamuel.elastic4s.source.DocumentSource

/**
 * A processor that expects json.
 */
trait JsonProcessor {
  def beforeAction()(implicit ec: ExecutionContext)
  def process(offset: Long, topic: String, json: String)(implicit ec: ExecutionContext)
}

/**
 * Simple interface to abstract out how we get messages from kafka
 */
trait MessageProcessor {
  def process(offset: Long, topic: String, message: Array[Byte])(implicit ec: ExecutionContext)
}

final case class PassthroughSource(json: String) extends DocumentSource