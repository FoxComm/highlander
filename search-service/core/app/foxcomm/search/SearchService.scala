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
    val withQuery = searchQuery match {
      case SearchQuery.es(query, _) ⇒ (_: SearchDefinition) rawQuery Json.fromJsonObject(query).noSpaces
      case SearchQuery.fc(query, _) ⇒
        (_: SearchDefinition) bool {
          query.query.foldLeft(new BoolQueryDefinition) {
            case (bool, QueryFunction.is(in, value)) ⇒
              bool.filter(in.toList.map(termsQuery(_, value.fold(QueryFunction.listOfAnyValueF): _*)))
            case (bool, _) ⇒ bool // TODO: implement rest of cases
          }
        }
    }
    val baseSearch = withQuery(search in searchIndex size searchSize)
    val limitedSearch =
      searchQuery.fields.fold(baseSearch)(fields ⇒ baseSearch sourceInclude (fields.toList: _*))
    client
      .execute(searchFrom.fold(limitedSearch)(limitedSearch from))
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
