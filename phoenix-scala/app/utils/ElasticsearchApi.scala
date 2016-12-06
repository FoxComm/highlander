package utils

import scala.collection.JavaConverters._
import scala.concurrent.Future

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, IndexAndType, RichSearchResponse}
import com.typesafe.config.Config
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.bucket.terms.{StringTerms, Terms}
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import utils.ElasticsearchApi._
import utils.aliases._

// TODO: move to Apis?
case class ElasticsearchApi(host: String, cluster: String, index: String)(implicit ec: EC) {

  val aggregationName = "my-unique-aggregation"
  val settings        = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client          = ElasticClient.transport(settings, ElasticsearchClientUri(host))

  private def getIndexAndType(searchView: SearchViewReference)(implicit au: AU): IndexAndType = {
    val resultIndex = if (searchView.scoped) s"${index}_${au.token.scope}" else index
    IndexAndType(resultIndex, searchView.typeName)
  }

  /**
    * Injects metrics aggregation by specified field name into prepared query
    */
  def checkMetrics(searchView: SearchViewReference,
                   query: Json,
                   fieldName: String,
                   references: Seq[String])(implicit au: AU): Future[Long] = {

    // Extract metrics data from aggregatino results
    def getDocCount(resp: RichSearchResponse): Long =
      resp.aggregations.getAsMap.asScala.get(aggregationName) match {
        case Some(agg) ⇒ agg.asInstanceOf[InternalFilter].getDocCount
        case _         ⇒ 0
      }

    val queryString = compact(render(query))

    val request =
      search in getIndexAndType(searchView) rawQuery queryString aggregations (
          aggregation filter aggregationName filter termsQuery(fieldName, references.toList: _*)
      ) size 0

    logQuery(request.show)
    client.execute(request).map(getDocCount)
  }

  /**
    * Injects bucket aggregation by specified field name into prepared query
    */
  def checkBuckets(searchView: SearchViewReference,
                   query: Json,
                   fieldName: String,
                   references: Seq[String])(implicit au: AU): Future[Buckets] = {

    def toBucket(bucket: Terms.Bucket): TheBucket =
      TheBucket(key = bucket.getKeyAsString, docCount = bucket.getDocCount)

    // Extract bucket data from aggregration results
    def getBuckets(resp: RichSearchResponse): Buckets =
      resp.aggregations.getAsMap.asScala.get(aggregationName) match {
        case Some(agg) ⇒ agg.asInstanceOf[StringTerms].getBuckets.asScala.map(toBucket)
        case _         ⇒ List.empty
      }

    val queryString = compact(render(query))

    val request =
      search in getIndexAndType(searchView) rawQuery queryString aggregations (
          aggregation terms aggregationName script s"doc['$fieldName'].value"
      ) size 0

    logQuery(request.show)
    client.execute(request).map(getBuckets)
  }

  /**
    * Render compact query for logging
    */
  private def logQuery(query: String): Unit =
    Console.out.println(s"Preparing Elasticsearch query: ${compact(render(parse(query)))}")
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

  case class SearchViewReference(typeName: String, scoped: Boolean)

  def fromConfig(config: Config)(implicit ec: EC): ElasticsearchApi =
    ElasticsearchApi(host = config.getString(hostKey),
                     cluster = config.getString(clusterKey),
                     index = config.getString(indexKey))

  def default()(implicit ec: EC): ElasticsearchApi =
    ElasticsearchApi(host = defaultHost, cluster = defaultCluster, index = defaultIndex)
}
