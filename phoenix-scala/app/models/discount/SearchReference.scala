package models.discount

import scala.concurrent.Future

import cats.data.Xor
import models.discount.SearchReference._
import models.sharedsearch.SharedSearches
import org.json4s.JsonAST.JObject
import services.Result
import utils.ElasticsearchApi.{Buckets, SearchViewReference}
import utils.aliases._

/**
  * Linking mechanism for qualifiers (also used in offers)
  */
sealed trait SearchReference[T] {
  val searchView: SearchViewReference
  val fieldName: String
  val pureResult: Result[T]
  val searchId: Int

  def query(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[T] = {
    val refs = references(input)
    if (refs.isEmpty) return pureResult

    SharedSearches.findOneById(searchId).run().flatMap {
      case Some(search) ⇒
        search.rawQuery \ "query" match {
          case query: JObject ⇒
            esSearch(query, refs).map(result ⇒ Xor.Right(result))
          case _ ⇒ pureResult
        }
      case _ ⇒ pureResult
    }
  }

  protected def references(input: DiscountInput): Seq[String]
  protected def esSearch(query: Json, refs: Seq[String])(implicit es: ES, au: AU): Future[T]
}

trait SearchBuckets extends SearchReference[Buckets] {
  val pureResult: Result[Buckets] = pureBuckets

  def esSearch(query: Json, refs: Seq[String])(implicit es: ES, au: AU): Future[Buckets] =
    es.checkBuckets(searchView, query, fieldName, refs)
}

trait SearchMetrics extends SearchReference[Long] {
  val pureResult: Result[Long] = pureMetrics

  def esSearch(query: Json, refs: Seq[String])(implicit es: ES, au: AU): Future[Long] =
    es.checkMetrics(searchView, query, fieldName, refs)
}

case class CustomerSearch(customerSearchId: Int) extends SearchMetrics {
  val searchId: Int                   = customerSearchId
  val searchView: SearchViewReference = customersSearchView
  val fieldName: String               = customersSearchField

  def references(input: DiscountInput): Seq[String] = Seq(input.cart.accountId).map(_.toString)
}

case class ProductSearch(productSearchId: Int) extends SearchBuckets {
  val searchId: Int                   = productSearchId
  val searchView: SearchViewReference = productsSearchView
  val fieldName: String               = productsSearchField

  def references(input: DiscountInput): Seq[String] =
    input.lineItems.map(_.productForm.id.toString)
}

case class ProductVariantSearch(productVariantSearchId: Int) extends SearchBuckets {
  val searchId: Int                   = productVariantSearchId
  val searchView: SearchViewReference = productVariantSearchView
  val fieldName: String               = productVariantSearchField

  def references(input: DiscountInput): Seq[String] = input.lineItems.map(_.productVariant.code)
}

object SearchReference {
  def customersSearchView: SearchViewReference =
    SearchViewReference("customers_search_view", scoped = false)
  def productsSearchView: SearchViewReference =
    SearchViewReference("products_search_view", scoped = true)
  def productVariantSearchView: SearchViewReference =
    SearchViewReference("product_variant_search_view", scoped = true)

  def customersSearchField: String      = "id"
  def productsSearchField: String       = "productId"
  def productVariantSearchField: String = "code"

  def pureMetrics: Result[Long]    = Future.successful(Xor.Right(0))
  def pureBuckets: Result[Buckets] = Future.successful(Xor.Right(List.empty))
}
