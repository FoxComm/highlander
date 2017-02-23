package consumer.elastic.mappings.admin

import com.sksamuel.elastic4s.ElasticDsl.{mapping ⇒ esMapping, _}
import com.sksamuel.elastic4s.mappings.FieldType._
import consumer.aliases._
import consumer.elastic.AvroTransformer
import consumer.elastic.mappings.dateFormat

final case class StoreCreditTransactionsSearchView()(implicit ec: EC) extends AvroTransformer {
  def topic() = "store_credit_transactions_view"
  def mapping() = esMapping(topic()).fields(
      // Adjustment
      field("id", IntegerType),
      field("debit", IntegerType),
      field("availableBalance", IntegerType),
      field("state", StringType).index("not_analyzed"),
      field("createdAt", DateType).format(dateFormat),
      // Store Credit
      field("storeCreditId", IntegerType),
      field("customerId", IntegerType),
      field("originType", StringType).index("not_analyzed"),
      field("currency", StringType).index("not_analyzed"),
      field("storeCreditCreatedAt", DateType).format(dateFormat),
      field("scope", StringType).index("not_analyzed"),
      // Order Payment
      field("orderPayment").nested(
          field("cordReferenceNumber", StringType).analyzer("upper_cased"),
          field("orderCreatedAt", DateType).format(dateFormat),
          field("orderPaymentCreatedAt", DateType).format(dateFormat)
      ),
      // Store Admins
      field("storeAdmin").nested(
          field("email", StringType).analyzer("autocomplete"),
          field("name", StringType).analyzer("autocomplete"),
          field("department", StringType).analyzer("autocomplete")
      )
  )

  override def nestedFields() = List("store_admin", "order_payment")
}
