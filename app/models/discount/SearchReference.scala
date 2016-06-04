package models.discount

import cats.data.Xor
import scala.concurrent.Future
import models.sharedsearch.SharedSearches
import services.Result
import SearchReference._
import utils.ElasticsearchApi.Buckets
import utils.aliases._

/**
  * Linking mechanism for qualifiers (also used in offers)
  */
sealed trait SearchReference[K, T] {
  val typeName: String
  val fieldName: String

  def references(input: DiscountInput): Seq[K]
  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[T]
}

case class CustomerSearch(customerSearchId: Int) extends SearchReference[Int, Long] {
  val typeName: String  = customersSearchView
  val fieldName: String = customersSearchField

  def references(input: DiscountInput): Seq[Int] = Seq(input.order.customerId)

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Long] = {
    SharedSearches.findOneById(customerSearchId).run().flatMap {
      case Some(search) ⇒
        es.checkMetrics(typeName, search.rawQuery, fieldName, references(input).map(_.toString))
          .map(result ⇒ Xor.Right(result))
      case _ ⇒
        pureMetrics
    }
  }
}

case class ProductSearch(productSearchId: Int) extends SearchReference[Int, Buckets] {
  val typeName: String  = productsSearchView
  val fieldName: String = productsSearchField

  def references(input: DiscountInput): Seq[Int] =
    input.lineItems.map(_.product.formId)

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Buckets] = {
    SharedSearches.findOneById(productSearchId).run().flatMap {
      case Some(search) ⇒
        es.checkBuckets(typeName, search.rawQuery, fieldName, references(input).map(_.toString))
          .map(res ⇒ Xor.Right(res))
      case _ ⇒
        pureBuckets
    }
  }
}

case class SkuSearch(skuSearchId: Int) extends SearchReference[String, Buckets] {
  val typeName: String  = skuSearchView
  val fieldName: String = skuSearchField

  def references(input: DiscountInput): Seq[String] = input.lineItems.map(_.sku.code)

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Buckets] = {
    SharedSearches.findOneById(skuSearchId).run().flatMap {
      case Some(search) ⇒
        es.checkBuckets(typeName, search.rawQuery, fieldName, references(input))
          .map(res ⇒ Xor.Right(res))
      case _ ⇒
        pureBuckets
    }
  }
}

object SearchReference {
  def customersSearchView: String = "customers_search_view"
  def productsSearchView: String  = "products_search_view"
  def skuSearchView: String       = "sku_search_view"

  def customersSearchField: String = "id"
  def productsSearchField: String  = "productId"
  def skuSearchField: String       = "code"

  def pureMetrics: Result[Long]    = Future.successful(Xor.Right(0))
  def pureBuckets: Result[Buckets] = Future.successful(Xor.Right(List.empty))
}
