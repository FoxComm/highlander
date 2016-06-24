package consumer.elastic

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._

object MappingHelpers {
  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  def address(name: String) = field(name).nested(
      field("address1", StringType).analyzer("autocomplete"),
      field("address2", StringType).analyzer("autocomplete"),
      field("city", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("zip", StringType).index("not_analyzed"),
      field("region", StringType).index("not_analyzed"),
      field("country", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("continent", StringType)
        .analyzer("autocomplete")
        .fields(field("raw", StringType).index("not_analyzed")),
      field("currency", StringType).analyzer("not_analyzed")
  )
}
