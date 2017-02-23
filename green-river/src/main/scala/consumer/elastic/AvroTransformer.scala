package consumer.elastic

import scala.concurrent.{Future, ExecutionContext}

import consumer.AvroJsonHelper

abstract class AvroTransformer(implicit ec: ExecutionContext) extends JsonTransformer {

  /**
    * List of fields containing JSON object, to be unescaped
    */
  def nestedFields(): List[String] = List.empty

  def topic(): String

  def transform(json: String): Future[Seq[Transformation]] = Future {
    Seq(Transformation(topic(), AvroJsonHelper.transformJson(json, nestedFields())))
  }
}
