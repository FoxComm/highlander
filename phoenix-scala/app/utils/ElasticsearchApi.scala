package utils

import scala.collection.JavaConverters._
import scala.concurrent.Future
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, IndexAndType, RichSearchResponse}
import com.typesafe.scalalogging.LazyLogging
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.terms.{StringTerms, Terms}
import org.json4s.JsonAST.{JArray, JObject, JString}
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import utils.ElasticsearchApi._
import utils.FoxConfig.ESConfig
import utils.aliases._

// TODO: move to Apis?
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
                   query: Json,
                   fieldName: String,
                   references: Seq[String]): Future[Long] = {

    if (references.isEmpty) return Future.successful(0)

    // Extract metrics data from aggregation results
    def getDocCount(resp: RichSearchResponse): Long =
      resp.aggregations.getAsMap.asScala.get(aggregationName) match {
        case Some(agg) ⇒ agg.asInstanceOf[InternalFilter].getDocCount
        case _         ⇒ 0
      }

    val queryString  = compact(render(query))
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
                   esQuery: Json,
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

    val newQuery     = injectFilterReferences(esQuery, fieldName, references)
    val queryString  = compact(render(newQuery))
    val indexAndType = getIndexAndType(searchView)

    val request = search in indexAndType rawQuery queryString aggregations (
          aggregation terms aggregationName script s"doc['$fieldName'].value"
      ) size 0

    logQuery(indexAndType, request.show)
    client.execute(request).map(getBuckets)
  }

  /**
    * Render compact query for logging
    */
  private def logQuery(indexAndType: IndexAndType, query: String): Unit = {
    logger.debug(
        s"Preparing Elasticsearch query to ${indexAndType.index}/${indexAndType.`type`}: ${compact(
        render(parse(query)))}")
  }

}

object ElasticsearchApi {

  case class TheBucket(key: String, docCount: Long)

  type Buckets = Seq[TheBucket]

  val hostKey    = "elasticsearch.host"
  val clusterKey = "elasticsearch.cluster"
  val indexKey   = "elasticsearch.index"

  val defaultHost    = "elasticsearch://localhost:9300"
  val defaultCluster = "elasticsearch"
  val defaultIndex   = "admin"

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

  def default()(implicit ec: EC): ElasticsearchApi = {
    val config = ESConfig(host = defaultHost, cluster = defaultCluster, index = defaultIndex)
    ElasticsearchApi(config)
  }

  protected def injectFilterReferences(query: Json,
                                       fieldName: String,
                                       references: Seq[String]): Json = {
    val refFilter     = JObject("terms" → JObject(fieldName → JArray(references.map(JString).toList)))
    val currentFilter = query \ "bool" \ "filter"
    currentFilter match {
      case singleFilter: JObject ⇒
        val newFilter = JArray(List(refFilter, singleFilter))
        query.replace(List("bool", "filter"), newFilter)
      case JArray(filters) ⇒
        val newFilter = JArray(List(refFilter) ++ filters)
        query.replace(List("bool", "filter"), newFilter)
      case _ ⇒
        val newQuery = JObject("bool" → JObject("filter" → JArray(List(refFilter))))
        query.merge(newQuery)
    }
  }
}
