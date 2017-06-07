package foxcomm.search.api

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finagle.Http
import com.twitter.util.Await
import foxcomm.search._
import foxcomm.utils.finch._
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import scala.concurrent.ExecutionContext

object Api extends App {
  def endpoint(searchService: SearchService)(implicit ec: ExecutionContext) =
    post(
      "search" :: string :: string :: param("size")
        .as[Int] :: paramOption("from").as[Int] :: jsonBody[SearchQuery]) {
      (searchIndex: String, searchType: String, size: Int, from: Option[Int], searchQuery: SearchQuery) =>
        searchService
          .searchFor(searchIndex / searchType, searchQuery, searchSize = size, searchFrom = from)
          .toTwitterFuture
          .map(Ok)
    }

  implicit val ec: ExecutionContext = ExecutionContext.global
  val config                        = AppConfig.load()
  val svc                           = SearchService.fromConfig(config)

  Await.result(
    Http.server
      .withStreaming(enabled = true)
      .serve(s"${config.http.interface}:${config.http.port}", endpoint(svc).toServiceAs[Application.Json]))
}
