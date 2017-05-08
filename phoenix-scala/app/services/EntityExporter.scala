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
import scala.annotation.tailrec
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
    implicit val chunkable = Chunkable.csvChunkable(payload.fields.map(_.displayName))

    val index = s"admin_${au.token.scope}" / entity.searchView
    val jsonSource = payload match {
      case ExportEntity.ByIDs(_, fields, ids) ⇒
        EntityExporter.export(
            searchIndex = index,
            searchFields = fields.map(_.name),
            searchIds = ids
        )
      case ExportEntity.BySearchQuery(_, fields, query) ⇒
        EntityExporter.export(
            searchIndex = index,
            searchFields = fields.map(_.name),
            searchQuery = query
        )
    }
    val csvSource = jsonSource.collect {
      case obj: JObject ⇒
        payload.fields.map {
          case ExportField(name, displayName) ⇒
            displayName → extractValue(name.split("\\.").toList, Some(obj)).getOrElse("")
        }
    }

    Http.renderAttachment(fileName = setName(payload, entity))(csvSource)
  }

  /** Extracts value from (possibly nested) field path.
    *
    * When there is something left in `fields` and `acc` is not a json object,
    * we simply omit outputting the value.
    */
  @tailrec private def extractValue(fields: List[String], acc: Option[JValue]): Option[String] = {
    def convert(jv: Option[JValue]) = jv.collect {
      case jn: JNumber ⇒ s"${jn.values}"
      case js: JString ⇒ s""""${js.values.replace("\"", "\"\"")}""""
    }

    (fields, acc) match {
      case (h :: t, Some(jobj: JObject)) ⇒ extractValue(t, jobj.obj.toMap.get(h))
      case (Nil, _)                      ⇒ convert(acc)
      case (_, _)                        ⇒ None
    }
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

    // As of 2.3.x elastic4s's fetchSourceContext on single get item is ignored in multi get request,
    // so we need to set it manually.
    // This time we dank mutability.
    val sourceCtx = new FetchSourceContext(searchFields.toArray)
    val query     = multiget(searchIds.map(get id _ from searchIndex))
    query._builder.request().getItems.asScala.foreach(_.fetchSourceContext(sourceCtx))

    // For the sake of api consistency we pretend here we have a stream of results.
    // It's not true, as multiget fetches all documents eagerly.
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
