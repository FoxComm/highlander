package consumer

import scala.concurrent.{Future, ExecutionContext}

import com.sksamuel.elastic4s.mappings.MappingDefinition

package object elastic {
  /**
    * Json transformer has two parts, the ES mapping definition and
    * a function that takes json and transforms it to another json
    * before it is saved to ES
    */
  trait JsonTransformer {
    def mapping() : MappingDefinition
    def transform(json: String) : Future[String]
  }

  abstract class AvroTransformer(implicit ec: ExecutionContext) extends JsonTransformer {
    def nestedFields(): List[String] = List.empty
    def transform(json: String): Future[String] = Future {
      AvroJsonHelper.transformJson(json, nestedFields())
    }
  }
}

