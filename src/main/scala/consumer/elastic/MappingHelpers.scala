package consumer.elastic

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

object MappingHelpers {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  def address(name: String) = field(name).nested(
    field("address1", StringType).analyzer("autocomplete"),
    field("address2", StringType).analyzer("autocomplete"),
    field("city", StringType).analyzer("autocomplete"),
    field("zip", StringType).index("not_analyzed"),
    field("region", StringType).analyzer("autocomplete"),
    field("country", StringType).analyzer("autocomplete"),
    field("continent", StringType).analyzer("autocomplete"),
    field("currency", StringType).analyzer("autocomplete")
  )
}
