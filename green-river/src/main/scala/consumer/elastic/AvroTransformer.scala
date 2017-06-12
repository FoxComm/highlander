package consumer.elastic

import scala.concurrent.{ExecutionContext, Future}

import consumer.AvroJsonHelper

abstract class AvroTransformer(implicit ec: ExecutionContext) extends JsonTransformer {

  /**
    * List of fields containing JSON object, to be unescaped
    */
  def nestedFields(): List[String] = List.empty

  def topic(): String

  def transform(json: String): Future[String] = Future {
    AvroJsonHelper.transformJson(json, nestedFields())
  }
}
