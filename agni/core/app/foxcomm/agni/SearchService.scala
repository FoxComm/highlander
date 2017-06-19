package foxcomm.agni

import scala.language.postfixOps
import cats.implicits._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import foxcomm.agni.dsl.query._
import io.circe._
import io.circe.jawn.parseByteBuffer
import org.elasticsearch.common.settings.Settings
import scala.concurrent.{ExecutionContext, Future}

class SearchService(private val client: ElasticClient) extends AnyVal {
  import SearchService.ExtractJsonObject

  def searchFor(searchIndex: IndexAndTypes,
                searchQuery: SearchPayload,
                searchSize: Int,
                searchFrom: Option[Int])(implicit ec: ExecutionContext): Future[SearchResult] = {
    val withQuery = searchQuery match {
      case SearchPayload.es(query, _) ⇒ (_: SearchDefinition) rawQuery Json.fromJsonObject(query).noSpaces
      case SearchPayload.fc(query, _) ⇒
        // TODO: this is really some basic and quite ugly interpreter
        // consider more principled approach
        // maybe free monad would be a good fit there?
        (_: SearchDefinition) bool {
          query
            .map(_.query.foldLeft(new BoolQueryDefinition) {
              case (bool, QueryFunction.eq(in, value)) ⇒
                bool.filter(in.toList.map(termsQuery(_, value.toList: _*)))
              case (bool, QueryFunction.neq(in, value)) ⇒
                bool.not(in.toList.map(termsQuery(_, value.toList: _*)))
              case (bool, QueryFunction.matches(in, value)) ⇒
                val fields = in.toList
                bool.must(value.toList.map(q ⇒ multiMatchQuery(q).fields(fields)))
              case (bool, QueryFunction.range(in, value)) ⇒
                val query   = rangeQuery(in.field)
                val unified = value.unify
                val queryWithLowerBound = unified.lower.fold(query) {
                  case (b, v) ⇒ query.from(v).includeLower(b.withBound)
                }
                val boundedQuery = unified.upper.fold(queryWithLowerBound) {
                  case (b, v) ⇒ queryWithLowerBound.to(v).includeUpper(b.withBound)
                }
                bool.filter(boundedQuery)
              case (bool, _) ⇒ bool // TODO: implement rest of cases
            })
            .getOrElse((new BoolQueryDefinition).must(matchAllQuery))
        }
    }
    val baseSearch = withQuery(search in searchIndex size searchSize)
    val limitedSearch =
      searchQuery.fields.fold(baseSearch)(fields ⇒ baseSearch sourceInclude (fields.toList: _*))
    client
      .execute(searchFrom.fold(limitedSearch)(limitedSearch from))
      .map(response ⇒
        SearchResult(
          result = response.hits.collect {
            case ExtractJsonObject(obj) ⇒ obj
          }(collection.breakOut),
          pagination = SearchPagination(total = response.totalHits),
          maxScore = response.maxScore
      ))
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
