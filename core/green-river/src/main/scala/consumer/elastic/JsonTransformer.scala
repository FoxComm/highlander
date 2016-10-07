package consumer.elastic

import scala.concurrent.Future

import com.sksamuel.elastic4s.mappings.MappingDefinition

/**
  * Json transformer has two parts, the ES mapping definition and
  * a function that takes json and transforms it to another json
  * before it is saved to ES
  */
trait JsonTransformer {
  def mapping(): MappingDefinition
  def transform(json: String): Future[String]
}
