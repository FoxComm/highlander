package utils

import com.typesafe.config.Config

import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import org.json4s.jackson.JsonMethods.{render, compact, parse}

import failures.ElasticsearchFailure
import services.Result
import utils.aliases._

case class ElasticsearchApi(host: String, cluster: String, index: String)(implicit ec: EC) {

  val aggregationFilterName = "qualifier"

  val settings = Settings.settingsBuilder().put("cluster.name", cluster).build()
  val client = ElasticClient.transport(settings, ElasticsearchClientUri(host))

  /**
    * Injects aggregation by specified field name into prepared query
    */
  def checkAggregation(typeName: String, query: String, fieldName: String, references: Seq[String]): Result[Long] = {
    val request = search in s"$index/$typeName" rawQuery query aggregations(
      aggregation filter aggregationFilterName filter termsQuery(fieldName, references.toList: _*)
    ) size 0

    logQuery(request.show)

    try {
      val future = client.execute(request).map { response ⇒
        response.aggregations.getAsMap.asScala.get(aggregationFilterName) match {
          case Some(q) ⇒ q.asInstanceOf[InternalFilter].getDocCount
          case _       ⇒ 0
        }
      }

      Result.fromFuture(future)
    } catch {
      case NonFatal(e) ⇒ Result.failure(ElasticsearchFailure(e.getMessage))
    }
  }

  /**
    * Render compact query for logging
    */
  private def logQuery(query: String): Unit = {
    Console.out.println(s"Requesting Elasticsearch with query: ${compact(render(parse(query)))}")
  }
}

object ElasticsearchApi {

  val hostKey     = "elasticsearch.host"
  val clusterKey  = "elasticsearch.cluster"
  val indexKey    = "elasticsearch.index"

  def fromConfig(config: Config)(implicit ec: EC): ElasticsearchApi = ElasticsearchApi(host = config.getString(hostKey),
    cluster = config.getString(clusterKey), index = config.getString(indexKey))
}
