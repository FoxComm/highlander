package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class GiftCardsSearchView()(implicit ec: EC) extends AvroTransformer {
  def mapping() = esMapping("gift_cards_search_view").fields(
      field("id", IntegerType),
      field("code", StringType).analyzer("upper_cased"),
      field("originType", StringType).index("not_analyzed"),
      field("state", StringType).index("not_analyzed"),
      field("currency", StringType).index("not_analyzed"),
      field("originalBalance", IntegerType),
      field("currentBalance", IntegerType),
      field("availableBalance", IntegerType),
      field("canceledAmount", IntegerType),
      field("scope", StringType).index("not_analyzed"),
      field("createdAt", DateType).format(dateFormat),
      field("updatedAt", DateType).format(dateFormat)
  )
}
