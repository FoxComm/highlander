package foxcomm.search

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import io.circe._
import org.elasticsearch.common.settings.Settings
import org.reactivestreams.Publisher

class SearchService(private val client: ElasticClient) extends AnyVal {
  def query(searchIndex: IndexAndTypes, searchQuery: SearchQuery)(
      implicit system: ActorSystem): Publisher[RichSearchHit] =
    client.publisher(
      search in searchIndex rawQuery Json
        .fromJsonObject(searchQuery.query)
        .noSpaces scroll "1m" sourceInclude (searchQuery.fields.toList: _*))
}

object SearchService {
  def apply(client: ElasticClient): SearchService = new SearchService(client)

  def fromConfig(config: AppConfig): SearchService = {
    val esConfig = config.elasticsearch
    val settings =
      Settings.settingsBuilder().put("cluster.name", esConfig.cluster).build()
    val client =
      ElasticClient.transport(settings, ElasticsearchClientUri(esConfig.host))

    new SearchService(client)
  }
}
