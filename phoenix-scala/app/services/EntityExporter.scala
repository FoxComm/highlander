package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexAndTypes
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import org.elasticsearch.search.fetch.source.FetchSourceContext
import org.json4s.DefaultReaders._
import org.json4s.JsonAST.JNumber
import org.json4s._
import org.json4s.jackson.{compactJson, parseJson}
import payloads.ExportEntity
import utils.aliases._
import utils.apis.Apis

/** Export entities from ElasticSearch with given fields using specified search type.
  *
  * Note that this implementation is quite stringly typed.
  * That shouldn't be that much problem,
  * as all those functions should just pass arguments to ElasticSearch and then result back to the client.
  */
object EntityExporter {
  def export(payload: ExportEntity, searchType: String)(
      implicit apis: Apis,
      au: AU,
      ec: EC,
      system: ActorSystem): Source[Json, NotUsed] = {
    val idx = s"admin_${au.token.scope}" / searchType
    payload match {
      case ExportEntity.UsingIDs(fields, ids) ⇒
        EntityExporter.export(
            searchIndex = idx,
            searchFields = fields,
            searchIds = ids
        )
      case ExportEntity.UsingSearchQuery(fields, query) ⇒
        EntityExporter.export(
            searchIndex = idx,
            searchFields = fields,
            searchQuery = query
        )
    }
  }

  def export(searchIndex: IndexAndTypes, searchFields: List[String], searchIds: List[Long])(
      implicit apis: Apis,
      au: AU,
      ec: EC): Source[Json, NotUsed] = {
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

  def export(searchIndex: IndexAndTypes, searchFields: List[String], searchQuery: Json)(
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
