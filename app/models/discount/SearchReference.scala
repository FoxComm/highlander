package models.discount

import cats.data.Xor
import scala.concurrent.Future
import models.sharedsearch.SharedSearches
import services.Result
import utils.aliases._

/**
  * Linking mechanism for qualifiers (also used in offers)
  */
sealed trait SearchReference {
  val typeName: String
  val fieldName: String

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Long]
}

case class CustomerSearch(customerSearchId: Int) extends SearchReference {
  val typeName: String  = "customers_search_view"
  val fieldName: String = "id"

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Long] = {
    SharedSearches.findOneById(customerSearchId).run().flatMap {
      case Some(sharedSearch) ⇒
        val future = es.checkAggregation(typeName = typeName,
                                         query = sharedSearch.rawQuery,
                                         fieldName = fieldName,
                                         references = Seq(input.order.customerId.toString))
        future.map(result ⇒ Xor.Right(result))
      case _ ⇒ Future.successful(Xor.Right(0))
    }
  }
}

case class ProductSearch(productSearchId: Int) extends SearchReference {
  val typeName: String  = "products_search_view"
  val fieldName: String = "product_id"

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Long] = {
    SharedSearches.findOneById(productSearchId).run().flatMap {
      case Some(sharedSearch) ⇒
        val future = es.checkAggregation(typeName = typeName,
                                         query = sharedSearch.rawQuery,
                                         fieldName = fieldName,
                                         references =
                                           input.lineItems.map(_.product.formId.toString))
        future.map(result ⇒ Xor.Right(result))
      case _ ⇒ Future.successful(Xor.Right(0))
    }
  }
}

case class SkuSearch(skuSearchId: Int) extends SearchReference {
  val typeName: String  = "skus_search_view"
  val fieldName: String = "code"

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Long] = {
    SharedSearches.findOneById(skuSearchId).run().flatMap {
      case Some(sharedSearch) ⇒
        val future = es.checkAggregation(typeName = typeName,
                                         query = sharedSearch.rawQuery,
                                         fieldName = fieldName,
                                         references = input.lineItems.map(_.sku.code))
        future.map(result ⇒ Xor.Right(result))
      case _ ⇒ Future.successful(Xor.Right(0))
    }
  }
}
