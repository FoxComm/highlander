package foxcomm.agni

import cats.implicits._
import foxcomm.agni.interpreter.es._
import io.circe._
import io.circe.jawn.parseByteBuffer
import monix.eval.{Coeval, Task}
import org.elasticsearch.action.search.{SearchAction, SearchRequest, SearchRequestBuilder, SearchResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.search.SearchHit

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class SearchService private (client: Client, interpreter: ESSearchInterpreter) {
  import SearchService.ExtractJsonObject

  def translate(searchPayload: SearchPayload): Task[Json] = {
    def buildJson(req: SearchRequest): Coeval[Json] =
      parseByteBuffer(req.source().toChannelBuffer.toByteBuffer)
        .fold(Coeval.raiseError(_), Coeval.eval(_))

    for {
      req  ← interpreter(searchPayload → new SearchRequestBuilder(client, SearchAction.INSTANCE)).task
      json ← buildJson(req).task
    } yield json
  }

  def searchFor(searchIndex: String,
                searchType: String,
                searchPayload: SearchPayload,
                searchSize: Int,
                searchFrom: Option[Int]): Task[SearchResult] = {
    def prepareBuilder: Coeval[SearchRequestBuilder] = Coeval.eval {
      val builder = new SearchRequestBuilder(client, SearchAction.INSTANCE)
      builder
        .setIndices(searchIndex)
        .setTypes(searchType)
        .setSize(searchSize)
      searchFrom.foreach(builder.setFrom)
      builder
    }

    def searchRequest: Task[SearchRequest] = prepareBuilder.flatMap(b ⇒ interpreter(searchPayload → b)).task

    for {
      request  ← searchRequest
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

  def apply(client: Client, interpreter: ESSearchInterpreter): SearchService =
    new SearchService(client, interpreter)

  def fromConfig(config: AppConfig, interpreter: ESSearchInterpreter): SearchService = {
    val esConfig = config.elasticsearch
    val settings =
      Settings.settingsBuilder().put("cluster.name", esConfig.cluster).build()
    val client = TransportClient
      .builder()
      .settings(settings)
      .build()
      .addTransportAddresses(esConfig.host.toList.map(new InetSocketTransportAddress(_)): _*)

    apply(client, interpreter)
  }
}
