package foxcomm.agni

import cats.implicits._
import foxcomm.agni.interpreter._
import io.circe._
import io.circe.jawn.parseByteBuffer
import monix.eval.{Coeval, Task}
import org.elasticsearch.action.search.{SearchAction, SearchRequestBuilder, SearchResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit
import scala.concurrent.ExecutionContext

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class SearchService private (client: Client)(implicit qi: QueryInterpreter[Coeval, BoolQueryBuilder]) {
  import SearchService.ExtractJsonObject

  def searchFor(searchIndex: String,
                searchType: String,
                searchQuery: SearchPayload,
                searchSize: Int,
                searchFrom: Option[Int])(implicit ec: ExecutionContext): Task[SearchResult] = {
    def setupBuilder: Coeval[SearchRequestBuilder] = Coeval.eval {
      val builder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
      builder
        .setIndices(searchIndex)
        .setTypes(searchType)
        .setSize(searchSize)
      searchFrom.foreach(builder.setFrom)
      searchQuery.fields.foreach(fs ⇒ builder.setFetchSource(fs.toList.toArray, Array.empty[String]))
      builder
    }

    def evalQuery(builder: SearchRequestBuilder): Coeval[SearchRequestBuilder] = searchQuery match {
      case SearchPayload.es(query, _) ⇒
        Coeval.eval(builder.setQuery(Printer.noSpaces.prettyByteBuffer(Json.fromJsonObject(query)).array()))
      case SearchPayload.fc(query, _) ⇒
        query.fold {
          Coeval.eval(builder.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())))
        } { q ⇒
          qi(QueryBuilders.boolQuery(), q.query).map(builder.setQuery)
        }
    }

    for {
      builder ← setupBuilder.flatMap(evalQuery).task
      request = builder.request()
      response ← async[SearchResponse, SearchResult](client.search(request, _))
    } yield {
      val hits = response.getHits
      SearchResult(
        result = hits
          .hits()
          .view
          .collect {
            case ExtractJsonObject(obj) ⇒ obj
          }
          .toList,
        pagination = SearchPagination(total = hits.totalHits()),
        maxScore = hits.getMaxScore
      )
    }
  }
}

object SearchService {
  object ExtractJsonObject {
    def unapply(hit: SearchHit): Option[JsonObject] =
      parseByteBuffer(hit.sourceRef.toChannelBuffer.toByteBuffer).toOption
        .flatMap(_.asObject)
  }

  def apply(client: Client)(implicit qi: QueryInterpreter[Coeval, BoolQueryBuilder]): SearchService =
    new SearchService(client)

  def fromConfig(config: AppConfig)(implicit qi: QueryInterpreter[Coeval, BoolQueryBuilder]): SearchService = {
    val esConfig = config.elasticsearch
    val settings =
      Settings.settingsBuilder().put("cluster.name", esConfig.cluster).build()
    val client = TransportClient
      .builder()
      .settings(settings)
      .build()
      .addTransportAddresses(esConfig.host.toList.map(new InetSocketTransportAddress(_)): _*)

    new SearchService(client)
  }
}
