package models.discount

import scala.concurrent.Future

import cats.data.Xor
import models.discount.SearchReference._
import models.sharedsearch.SharedSearches
import services.Result
import utils.ElasticsearchApi.{Buckets, SearchViewReference}
import utils.aliases._

/**
  * Linking mechanism for qualifiers (also used in offers)
  */
sealed trait SearchReference[K, T] {
  val searchView: SearchViewReference
  val fieldName: String

  def references(input: DiscountInput): Seq[K]
  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[T]
}

case class CustomerSearch(customerSearchId: Int) extends SearchReference[Int, Long] {
  val searchView: SearchViewReference = customersSearchView
  val fieldName: String               = customersSearchField

  def references(input: DiscountInput): Seq[Int] = Seq(input.cart.accountId)

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Long] = {
    SharedSearches.findOneById(customerSearchId).run().flatMap {
      case Some(search) ⇒
        es.checkMetrics(searchView, search.rawQuery, fieldName, references(input).map(_.toString))
          .map(result ⇒ Xor.Right(result))
      case _ ⇒
        pureMetrics
    }
  }
}

case class ProductSearch(productSearchId: Int) extends SearchReference[Int, Buckets] {
  val searchView: SearchViewReference = productsSearchView
  val fieldName: String               = productsSearchField

  def references(input: DiscountInput): Seq[Int] =
    input.lineItems.map(_.productForm.id)

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Buckets] = {
    SharedSearches.findOneById(productSearchId).run().flatMap {
      case Some(search) ⇒
        es.checkBuckets(searchView, search.rawQuery, fieldName, references(input).map(_.toString))
          .map(res ⇒ Xor.Right(res))
      case _ ⇒
        pureBuckets
    }
  }
}

case class SkuSearch(skuSearchId: Int) extends SearchReference[String, Buckets] {
  val searchView: SearchViewReference = skuSearchView
  val fieldName: String               = skuSearchField

  def references(input: DiscountInput): Seq[String] = input.lineItems.map(_.variant.code)

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Buckets] = {
    SharedSearches.findOneById(skuSearchId).run().flatMap {
      case Some(search) ⇒
        es.checkBuckets(searchView, search.rawQuery, fieldName, references(input))
          .map(res ⇒ Xor.Right(res))
      case _ ⇒
        pureBuckets
    }
  }
}

object SearchReference {
  def customersSearchView: SearchViewReference =
    SearchViewReference("customers_search_view", scoped = false)
  def productsSearchView: SearchViewReference =
    SearchViewReference("products_search_view", scoped = true)
  def skuSearchView: SearchViewReference = SearchViewReference("sku_search_view", scoped = true)

  def customersSearchField: String = "id"
  def productsSearchField: String  = "productId"
  def skuSearchField: String       = "code"

  def pureMetrics: Result[Long]    = Future.successful(Xor.Right(0))
  def pureBuckets: Result[Buckets] = Future.successful(Xor.Right(List.empty))
}
