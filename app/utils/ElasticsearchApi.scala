package utils

import scala.concurrent.Future

import com.typesafe.config.Config
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri, RichSearchResponse}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import scala.collection.JavaConverters._

import org.json4s.JsonAST.JValue
import org.json4s.jackson.JsonMethods.{compact, parse, render}
import utils.aliases._

case class ElasticsearchApi(host: String, cluster: String, index: String)(implicit ec: EC) {

  val aggregationName = "my-unique-aggregation"

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client   = ElasticClient.transport(settings, ElasticsearchClientUri(host))

  /**
    * Injects aggregation by specified field name into prepared query
    */
  def checkAggregation(typeName: String,
                       query: JValue,
                       fieldName: String,
                       references: Seq[String]): Future[Long] = {
    // Extract matched document count from aggregation
    def getDocCount(resp: RichSearchResponse): Long =
      resp.aggregations.getAsMap.asScala.get(aggregationName) match {
        case Some(q) ⇒ q.asInstanceOf[InternalFilter].getDocCount
        case _       ⇒ 0
      }

    val queryString = compact(render(query))

    val request =
      search in s"$index/$typeName" rawQuery queryString aggregations (
          aggregation filter aggregationName filter termsQuery(fieldName, references.toList: _*)
      ) size 0

    logQuery(request.show)
    client.execute(request).map(getDocCount)
  }

  /**
    * Render compact query for logging
    */
  private def logQuery(query: String): Unit =
    Console.out.println(s"Preparing Elasticsearch query: ${compact(render(parse(query)))}")
}

object ElasticsearchApi {

  val hostKey    = "elasticsearch.host"
  val clusterKey = "elasticsearch.cluster"
  val indexKey   = "elasticsearch.index"

  val defaultHost    = "elasticsearch://localhost:9300"
  val defaultCluster = "elasticsearch"
  val defaultIndex   = "phoenix"

  def fromConfig(config: Config)(implicit ec: EC): ElasticsearchApi =
    ElasticsearchApi(host = config.getString(hostKey),
                     cluster = config.getString(clusterKey),
                     index = config.getString(indexKey))

  def default()(implicit ec: EC): ElasticsearchApi =
    ElasticsearchApi(host = defaultHost, cluster = defaultCluster, index = defaultIndex)
}
