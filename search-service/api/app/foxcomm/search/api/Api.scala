package foxcomm.search.api

import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finagle.Http
import com.twitter.util.Await
import foxcomm.utils.finch._
import foxcomm.search._
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import scala.concurrent.ExecutionContext


object Api extends App {
  def intParam(name: String): Endpoint[Int] = param(name).as[Int]

  def optIntParam(name: String): Endpoint[Option[Int]] =
    paramOption(name).as[Int]

  def endpoint(searchService: SearchService)(implicit ec: ExecutionContext) =
    post("search" :: string :: string :: intParam("size") :: optIntParam(
      "from") :: jsonBody[SearchQuery]) {
      (searchIndex: String,
        searchType: String,
        size: Int,
        from: Option[Int],
        searchQuery: SearchQuery) =>
        searchService
          .searchFor(searchIndex / searchType,
            searchQuery,
            searchSize = size,
            searchFrom = from)
          .toTwitterFuture
          .map(Ok)
    }

  implicit val ec: ExecutionContext = ExecutionContext.global
  val config = AppConfig.load()
  val svc = SearchService.fromConfig(config)

  Await.result(
    Http.server
      .withStreaming(enabled = true)
      .serve(s"${config.http.interface}:${config.http.port}",
        endpoint(svc).toServiceAs[Application.Json]))
}
