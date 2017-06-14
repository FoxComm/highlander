package foxcomm.search

import scala.language.postfixOps
import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import io.circe._
import io.circe.jawn.parseByteBuffer
import org.elasticsearch.common.settings.Settings
import scala.concurrent.{ExecutionContext, Future}

class SearchService(private val client: ElasticClient) extends AnyVal {
  import SearchService.ExtractJsonObject

  def searchFor(searchIndex: IndexAndTypes,
                searchQuery: SearchQuery,
                searchSize: Int,
                searchFrom: Option[Int])(implicit ec: ExecutionContext): Future[SearchResult] = {
    val baseQuery = search in searchIndex size searchSize rawQuery Json
      .fromJsonObject(searchQuery.query)
      .noSpaces
    val query = searchQuery.fields.fold(baseQuery)(fields ⇒ baseQuery sourceInclude (fields.toList: _*))
    client
      .execute(searchFrom.fold(query)(query from))
      .map(response ⇒
        SearchResult(result = response.hits.collect {
          case ExtractJsonObject(obj) ⇒ obj
        }(collection.breakOut), pagination = SearchPagination(total = response.totalHits)))
  }
}

object SearchService {
  object ExtractJsonObject {
    def unapply(hit: RichSearchHit): Option[JsonObject] =
      parseByteBuffer(hit.sourceRef.toChannelBuffer.toByteBuffer).toOption
        .flatMap(_.asObject)
  }

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
