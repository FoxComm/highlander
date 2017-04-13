package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}
import org.elasticsearch.search.fetch.source.FetchSourceContext
import org.json4s.JsonAST.{JNumber, JString}
import org.json4s._
import org.json4s.jackson.{compactJson, parseJson}
import payloads.ExportEntityPayloads._
import utils.Chunkable
import utils.aliases._
import utils.apis.Apis
import utils.http.Http

/** Export entities from ElasticSearch with given fields using specified search type.
  *
  * Note that this implementation is quite stringly typed.
  * That shouldn't be that much problem,
  * as all those functions should just pass arguments to ElasticSearch and then result back to the client.
  */
object EntityExporter {
  private[this] val formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"))

  def export(payload: ExportEntity, entity: ExportableEntity)(
      implicit apis: Apis,
      au: AU,
      ec: EC,
      system: ActorSystem): HttpResponse = {
    implicit val chunkable = Chunkable.csvChunkable(payload.fields)

    val index = s"admin_${au.token.scope}" / entity.searchView
    val jsonSource = payload match {
      case ExportEntity.ByIDs(_, fields, ids) ⇒
        EntityExporter.export(
            searchIndex = index,
            searchFields = fields,
            searchIds = ids
        )
      case ExportEntity.BySearchQuery(_, fields, query) ⇒
        EntityExporter.export(
            searchIndex = index,
            searchFields = fields,
            searchQuery = query
        )
    }
    val csvSource = jsonSource.collect {
      case obj: JObject ⇒
        val objFields = obj.obj.toMap
        payload.fields.map { field ⇒
          objFields
            .get(field)
            .collect {
              case jn: JNumber ⇒ field → s"${jn.values}"
              case js: JString ⇒ field → s""""${js.values.replace("\"", "\"\"")}""""
            }
            .getOrElse(field → "")
        }
    }

    Http.renderAttachment(fileName = setName(payload, entity))(csvSource)
  }

  private def setName(payload: ExportEntity, entity: ExportableEntity): String = {
    val date        = formatter.format(Instant.now)
    val description = payload.description.map(_.trim.replaceAll("\\s+", "-"))

    (List(entity.entity) ++ description ++ List(date)).mkString("", "-", ".csv")
  }

  private def export(
      searchIndex: IndexAndTypes,
      searchFields: List[String],
      searchIds: List[Long])(implicit apis: Apis, au: AU, ec: EC): Source[Json, NotUsed] = {
    import scala.collection.JavaConverters._

    // as of 2.3.x elastic4s's fetchSourceContext on single get item is ignored in multi get request
    // so we need to set it manually
    // this time we dank mutability
    val sourceCtx = new FetchSourceContext(searchFields.toArray)
    val query     = multiget(searchIds.map(get id _ from searchIndex))
    query._builder.request().getItems.asScala.foreach(_.fetchSourceContext(sourceCtx))

    // for sake of api consistency we pretend here we have a stream of results
    // it's not true as multiget fetches all documents eagerly
    Source
      .fromFuture(apis.elasticSearch.client.execute(query))
      .map(_.responses.flatMap(_.response.map(_.getSourceAsString).map(parseJson(_))).toStream)
      .map(Source.apply)
      .flatMapConcat(identity)
  }

  private def export(searchIndex: IndexAndTypes, searchFields: List[String], searchQuery: Json)(
      implicit apis: Apis,
      au: AU,
      system: ActorSystem): Source[Json, NotUsed] =
    Source
      .fromPublisher(
          apis.elasticSearch.client.publisher(search in searchIndex rawQuery compactJson(
                  searchQuery) sourceInclude (searchFields: _*) scroll "1m"))
      .map(_.getSourceAsString)
      .map(parseJson(_))
}
