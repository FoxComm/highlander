package phoenix.models.discount

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.db._
import org.json4s.JsonAST.JObject
import phoenix.models.discount.SearchReference._
import phoenix.models.sharedsearch.SharedSearches
import phoenix.utils.ElasticsearchApi.{Buckets, ScopedSearchView, SearchView}
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

import scala.concurrent.Future

/**
  * Linking mechanism for qualifiers (also used in offers)
  */
sealed trait SearchReference[T] {
  val fieldName: String
  def pureResult(implicit ec: EC): Result[T]
  val searchId: Int

  def query(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis): Result[T] = {
    val refs = references(input)

    if (refs.isEmpty) pureResult
    else {
      for {
        searchO ← Result
                   .fromF(SharedSearches.findOneById(searchId).run()) // FIXME: why are we using .run here? And too verbose @michalrus
        result ← searchO match {
                  case Some(search) ⇒
                    search.rawQuery \ "query" match {
                      case query: JObject ⇒
                        val searchView = searchViewByScope(search.accessScope)
                        Result.fromF(esSearch(searchView, query, refs))
                      case _ ⇒ pureResult
                    }
                  case _ ⇒ pureResult
                }
      } yield result
    }
  }

  protected val searchViewByScope: (LTree ⇒ SearchView)
  protected def references(input: DiscountInput): Seq[String]
  protected def esSearch(searchView: SearchView, query: Json, refs: Seq[String])(
      implicit apis: Apis): Future[T]
}

trait SearchBuckets extends SearchReference[Buckets] {
  def pureResult(implicit ec: EC): Result[Buckets] = pureBuckets

  def esSearch(searchView: SearchView, query: Json, refs: Seq[String])(implicit apis: Apis): Future[Buckets] =
    apis.elasticSearch.checkBuckets(searchView, query, fieldName, refs)
}

trait SearchMetrics extends SearchReference[Long] {
  def pureResult(implicit ec: EC): Result[Long] = pureMetrics

  def esSearch(searchView: SearchView, query: Json, refs: Seq[String])(implicit apis: Apis): Future[Long] =
    apis.elasticSearch.checkMetrics(searchView, query, fieldName, refs)
}

case class CustomerSearch(customerSearchId: Int) extends SearchMetrics {
  val searchId: Int     = customerSearchId
  val searchViewByScope = searchView(customersSearchView)
  val fieldName: String = customersSearchField

  def references(input: DiscountInput): Seq[String] = Seq(input.customerAccountId).map(_.toString)
}

case class ProductSearch(productSearchId: Int) extends SearchBuckets {
  val searchId: Int     = productSearchId
  val searchViewByScope = scopedSearchView(productsSearchView)
  val fieldName: String = productsSearchField

  def references(input: DiscountInput): Seq[String] =
    input.lineItems.map(_.productId.toString)
}

case class SkuSearch(skuSearchId: Int) extends SearchBuckets {
  val searchId: Int     = skuSearchId
  val searchViewByScope = scopedSearchView(skuSearchView)
  val fieldName: String = skuSearchField

  def references(input: DiscountInput): Seq[String] = input.lineItems.map(_.skuCode)
}

object SearchReference {
  val customersSearchView: String = "customers_search_view"
  val productsSearchView: String  = "products_search_view"
  val skuSearchView: String       = "sku_search_view"

  def scopedSearchView(view: String): (LTree ⇒ SearchView) = { scope: LTree ⇒
    ScopedSearchView(view, scope.toString)
  }

  def searchView(view: String): (LTree ⇒ SearchView) = { _ ⇒
    SearchView(view)
  }

  def customersSearchField: String = "id"
  def productsSearchField: String  = "productId"
  def skuSearchField: String       = "code"

  def pureMetrics(implicit ec: EC): Result[Long]    = 0L.pure[Result]
  def pureBuckets(implicit ec: EC): Result[Buckets] = (Seq.empty: Buckets).pure[Result]
}
