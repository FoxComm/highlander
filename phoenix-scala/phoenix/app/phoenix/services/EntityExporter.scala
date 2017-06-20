package phoenix.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.Source
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import org.elasticsearch.search.fetch.source.FetchSourceContext
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import phoenix.payloads.EntityExportPayloads._
import phoenix.utils.Chunkable
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.Http
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.control.Exception._
import core.utils.Strings._

/** Export entities from ElasticSearch with given path using specified search type.
  *
  * Note that this implementation is quite stringly typed.
  * That shouldn't be that much problem,
  * as all those functions should just pass arguments to ElasticSearch and then result back to the client.
  */
object EntityExporter {
  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"))

  private val FieldMatcher = "(\\w+)(\\[-?\\d+\\])?".r

  private object ArrayElement {
    def unapply(field: String): Option[Int] =
      catching(classOf[NumberFormatException]).opt(field.stripPrefix("[").stripSuffix("]").toInt)
  }

  def export(
      payload: ExportEntity,
      entity: ExportableEntity)(implicit apis: Apis, au: AU, ec: EC, system: ActorSystem): HttpResponse = {
    implicit val chunkable = Chunkable.csvChunkable(payload.fields.map(_.displayName))

    val index = s"admin_${au.token.scope}" / entity.searchView
    val fields = entity.extraFields
      .map(getPath(_, removeArrayIndices = true).mkString(".")) ::: payload.fields.map {
      case ExportField(name, _) ⇒
        getPath(name, removeArrayIndices = true)
          .mkString(".") // we don't care about array index if it exists
    }
    val jsonSource = payload match {
      case ExportEntity.ByIDs(_, _, ids) ⇒
        EntityExporter.export(index, fields, ids)
      case ExportEntity.BySearchQuery(_, _, query, sort) ⇒
        EntityExporter.export(index, fields, query, sort)
    }
    val csvSource = jsonSource.collect {
      case obj: JObject ⇒
        payload.fields.map {
          case ExportField(name, displayName) if entity.calculateFields.isDefinedAt((name, obj)) ⇒
            (displayName, entity.calculateFields((name, obj)))
          case ExportField(name, displayName) ⇒
            (displayName, extractValue(getPath(name, removeArrayIndices = false), Some(obj)).getOrElse(""))
        }

    }

    Http.renderAttachment(fileName = setName(payload, entity))(csvSource)
  }

  private def getPath(field: String, removeArrayIndices: Boolean): List[String] =
    if (field.nonEmpty) {
      val path = field.split("\\.")

      path.flatMap {
        // optional group returns `null` if not matched, so we must guard it in option
        case FieldMatcher(name, idx) ⇒
          val tail = if (removeArrayIndices) Nil else Option(idx).toList
          name :: tail
        case _ ⇒ Nil
      }(collection.breakOut)
    } else Nil

  /** Extracts value from (possibly nested) field path.
    *
    * When there is something left in `path` and `acc` is not a json object,
    * we simply omit outputting the value.
    */
  @tailrec private def extractValue(path: List[String], acc: Option[JValue]): Option[String] =
    (path, acc) match {
      case (h :: t, Some(jobj: JObject)) ⇒ extractValue(t, jobj.obj.toMap.get(h))
      case (ArrayElement(i) :: t, Some(jarr: JArray)) ⇒
        extractValue(t,
                     catching(classOf[IndexOutOfBoundsException])
                       .opt(if (i >= 0) jarr(i) else jarr(jarr.arr.length + i)))
      case (Nil, _) ⇒
        acc.collect {
          case jn @ (_: JNumber | _: JBool) ⇒ jn.values.toString
          case jv: JString                  ⇒ jv.values.quote('"')
        }
      case (_, _) ⇒ None
    }

  private def setName(payload: ExportEntity, entity: ExportableEntity): String = {
    val date        = formatter.format(Instant.now)
    val description = payload.description.map(_.trim.replaceAll("\\s+", "-"))

    (List(entity.entity) ++ description ++ List(date)).mkString("", "-", ".csv")
  }

  private def export(searchIndex: IndexAndTypes,
                     searchFields: List[String],
                     searchIds: List[Long])(implicit apis: Apis, au: AU, ec: EC): Source[Json, NotUsed] = {
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
      .map(
        _.responses
          .flatMap(_.response.map(_.getSourceAsString).map(parseOpt(_).getOrElse(JObject())))
          .toStream)
      .map(Source.apply)
      .flatMapConcat(identity)
  }

  private def export(searchIndex: IndexAndTypes,
                     searchFields: List[String],
                     searchQuery: JObject,
                     searchSort: Option[List[RawSortDefinition]])(
      implicit apis: Apis,
      au: AU,
      system: ActorSystem): Source[Json, NotUsed] = {
    val rawQuery = search in searchIndex rawQuery compact(render(searchQuery))
    val query = searchSort match {
      case Some(sorts @ (_ :: _)) ⇒ rawQuery sort (sorts: _*)
      case _                      ⇒ rawQuery
    }
    Source
      .fromPublisher(apis.elasticSearch.client.publisher(query sourceInclude (searchFields: _*) scroll "1m"))
      .map(_.getSourceAsString)
      .map(parseOpt(_).getOrElse(JObject()))
  }
}
