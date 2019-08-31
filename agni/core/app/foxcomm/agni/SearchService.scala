package foxcomm.agni

import cats.implicits._
import foxcomm.agni.interpreter.es._
import io.circe._
import io.circe.jawn.parseByteBuffer
import monix.eval.{Coeval, Task}
import org.elasticsearch.action.search.{SearchAction, SearchRequestBuilder, SearchResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.{ToXContent, XContentFactory}
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.SearchHit

@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class SearchService private (client: Client, qi: ESQueryInterpreter) {
  import SearchService.ExtractJsonObject

  def translate(searchPayload: SearchPayload.fc): Task[Json] = {
    def buildJson(qb: QueryBuilder): Coeval[Json] =
      Coeval.eval {
        val builder = XContentFactory.jsonBuilder()
        builder.prettyPrint()
        builder.startObject()
        builder.field("query")
        qb.toXContent(builder, ToXContent.EMPTY_PARAMS)
        builder.endObject()
        parseByteBuffer(builder.bytes().toChannelBuffer.toByteBuffer)
          .fold(Coeval.raiseError(_), Coeval.eval(_))
      }.flatten

    for {
      builder ← qi(searchPayload.query).task
      json    ← buildJson(builder).task
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
      searchPayload.fields.foreach(fs ⇒ builder.setFetchSource(fs.toList.toArray, Array.empty[String]))
      builder
    }

    def evalQuery(builder: SearchRequestBuilder): Coeval[SearchRequestBuilder] = searchPayload match {
      case SearchPayload.es(query, _) ⇒
        Coeval.eval(builder.setQuery(Json.fromJsonObject(query).toBytes))
      case SearchPayload.fc(query, _) ⇒
        qi(query).map(builder.setQuery)
    }

    def setupBuilder: Task[SearchRequestBuilder] = (prepareBuilder flatMap evalQuery).task

    for {
      builder ← setupBuilder
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

  def apply(client: Client, qi: ESQueryInterpreter): SearchService =
    new SearchService(client, qi)

  def fromConfig(config: AppConfig, qi: ESQueryInterpreter): SearchService = {
    val esConfig = config.elasticsearch
    val settings =
      Settings.settingsBuilder().put("cluster.name", esConfig.cluster).build()
    val client = TransportClient
      .builder()
      .settings(settings)
      .build()
      .addTransportAddresses(esConfig.host.toList.map(new InetSocketTransportAddress(_)): _*)

    apply(client, qi)
  }
}
