package models.discount

import services.Result
import utils.aliases._

/**
  * Linking mechanism for qualifiers (also used in offers)
  */
sealed trait SearchReference {
  val typeName: String
  val fieldName: String
}

case class CustomerSearch(customerId: Int) extends SearchReference {
  val typeName: String  = "customers_search_view"
  val fieldName: String = "id"
}

case class ProductSearch(formId: Int) extends SearchReference {
  val typeName: String  = "products_search_view"
  val fieldName: String = "product_id"
}

case class SkuSearch(code: String) extends SearchReference {
  val typeName: String  = "skus_search_view"
  val fieldName: String = "code"
}

object SearchReference {

  val dummyQuery = """{"query": {"match_all": {}}"""

  def query(input: DiscountInput, search: SearchReference)(implicit ec: EC, es: ES): Result[Long] =
    search match {
      case CustomerSearch(customerId) ⇒
        val references = Seq(customerId.toString)
        es.checkAggregation(search.typeName, dummyQuery, search.fieldName, references)
      case ProductSearch(formId) ⇒
        val references = input.lineItems.map(_.product.formId.toString)
        es.checkAggregation(search.typeName, dummyQuery, search.fieldName, references)
      case SkuSearch(code) ⇒
        val references = input.lineItems.map(_.sku.code)
        es.checkAggregation(search.typeName, dummyQuery, search.fieldName, references)
    }
}
