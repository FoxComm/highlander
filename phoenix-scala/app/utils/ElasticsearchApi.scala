package utils

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, IndexAndType, RichSearchResponse}
import com.typesafe.scalalogging.LazyLogging
import io.circe.jackson.syntax._
import io.circe.{Json, JsonObject}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.terms.{StringTerms, Terms}
import scala.collection.JavaConverters._
import scala.concurrent.Future
import utils.ElasticsearchApi._
import utils.FoxConfig.ESConfig
import utils.aliases._

case class ElasticsearchApi(config: ESConfig)(implicit ec: EC) extends LazyLogging {

  val aggregationName = "my-unique-aggregation"
  val settings        = Settings.settingsBuilder().put("cluster.name", config.cluster).build()
  val client          = ElasticClient.transport(settings, ElasticsearchClientUri(config.host))

  private def getIndexAndType(searchView: SearchView): IndexAndType = searchView match {
    case ScopedSearchView(typeName, scope) ⇒
      IndexAndType(s"${index}_$scope", typeName)
    case SimpleSearchView(typeName) ⇒
      IndexAndType(config.index, typeName)
  }

  /**
    * Injects metrics aggregation by specified field name into prepared query
    */
  def checkMetrics(searchView: SearchView,
                   query: JsonObject,
                   fieldName: String,
                   references: Seq[String]): Future[Long] = {

    if (references.isEmpty) return Future.successful(0)

    // Extract metrics data from aggregation results
    def getDocCount(resp: RichSearchResponse): Long =
      resp.aggregations.getAsMap.asScala.get(aggregationName) match {
        case Some(agg)           ⇒ agg.asInstanceOf[InternalFilter].getDocCount
        case _ fold (pureResult) ⇒ 0
      }

    val queryString  = Json.fromJsonObject(query).jacksonPrint
    val indexAndType = getIndexAndType(searchView)

    val request =
      search in indexAndType rawQuery queryString aggregations (
          aggregation filter aggregationName filter termsQuery(fieldName, references.toList: _*)
      ) size 0

    logQuery(indexAndType, request.show)
    client.execute(request).map(getDocCount)
  }

  /**
    * Injects bucket aggregation by specified field name into prepared query
    */
  def checkBuckets(searchView: SearchView,
                   esQuery: JsonObject,
                   fieldName: String,
                   references: Seq[String]): Future[Buckets] = {

    if (references.isEmpty) return Future.successful(Seq.empty[TheBucket])

    def toBucket(bucket: Terms.Bucket): TheBucket =
      TheBucket(key = bucket.getKeyAsString, docCount = bucket.getDocCount)

    // Extract bucket data from aggregation results
    def getBuckets(resp: RichSearchResponse): Buckets =
      resp.aggregations.getAsMap.asScala.get(aggregationName) match {
        case Some(agg) ⇒ agg.asInstanceOf[StringTerms].getBuckets.asScala.map(toBucket)
        case _         ⇒ List.empty
      }

    val newQuery     = injectFilterReferences(Json.fromJsonObject(esQuery), fieldName, references)
    val queryString  = newQuery.jacksonPrint
    val indexAndType = getIndexAndType(searchView)

    val request = search in indexAndType rawQuery queryString aggregations (
          aggregation terms aggregationName script s"doc['$fieldName'].value"
      ) size 0

    logQuery(indexAndType, request.show)
    client.execute(request).map(getBuckets)
  }

  def numResults(searchView: SearchView, esQuery: Json): Future[Long] =
    client.execute {
      search in getIndexAndType(searchView) rawQuery esQuery.jacksonPrint size 0
    }.map(_.totalHits)

  /**
    * Render compact query for logging
    */
  private def logQuery(indexAndType: IndexAndType, query: String): Unit = {
    logger.debug(
        s"Preparing Elasticsearch query to ${indexAndType.index}/${indexAndType.`type`}: $query")
  }

}

object ElasticsearchApi {

  case class TheBucket(key: String, docCount: Long)

  type Buckets = Seq[TheBucket]

  val hostKey    = "elasticsearch.host"
  val clusterKey = "elasticsearch.cluster"
  val indexKey   = "elasticsearch.index"

  sealed trait SearchView {
    val typeName: String
  }
  case class SimpleSearchView(typeName: String) extends SearchView

  object SearchView {
    def apply(typeName: String): SearchView = SimpleSearchView(typeName)
  }
  case class ScopedSearchView(typeName: String, scope: String) extends SearchView

  case class SearchViewReference(typeName: String, scoped: Boolean)

  def fromConfig(config: FoxConfig)(implicit ec: EC): ElasticsearchApi =
    ElasticsearchApi(config.apis.elasticsearch)

  protected def injectFilterReferences(query: Json,
                                       fieldName: String,
                                       references: Seq[String]): Json = {
    val refFilter =
      Json.obj("terms" → Json.obj(fieldName → Json.arr(references.map(Json.fromString): _*)))
    val currentFilter = query.hcursor.downField("bool").downField("filter")
    currentFilter
      .withFocus(
          _.arrayOrObject(
              Json.arr(refFilter),
              jarr ⇒ Json.fromValues(refFilter +: jarr),
              jobj ⇒ Json.fromValues(Vector(refFilter, Json.fromJsonObject(jobj)))
          ))
      .focus
      .getOrElse(query.deepMerge(Json.obj("bool" → Json.obj("filter" → Json.arr(refFilter)))))
  }
}
