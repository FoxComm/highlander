package foxcomm.search

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.twitter.finagle.Http
import com.twitter.finagle.netty3.ChannelBufferBuf
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import monix.execution.Scheduler

object FinchAPI {
  def intParam(name: String): Endpoint[Int] = param(name).as[Int]

  def optIntParam(name: String): Endpoint[Option[Int]] =
    paramOption(name).as[Int]

  def endpoint(searchService: SearchService)(implicit scheduler: Scheduler,
                                             system: ActorSystem) =
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
    } :+: post("streamSearch" :: string :: string :: jsonBody[SearchQuery]) {
      (searchIndex: String, searchType: String, searchQuery: SearchQuery) =>
        Ok(
          searchService
            .streamSearchFor(searchIndex / searchType, searchQuery)
            .toTwitterAsyncStream
            .map(_.sourceRef.toChannelBuffer)
            .map(ChannelBufferBuf.Owned(_)))
    }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("FinchAPI")
    implicit val scheduler: Scheduler = Scheduler(system.dispatcher)

    val config = AppConfig.load()
    val svc = SearchService.fromConfig(config)

    Await.result(
      Http.server
        .withStreaming(enabled = true)
        .serve(s"${config.http.interface}:${config.http.port}",
               endpoint(svc).toServiceAs[Application.Json]))
  }
}
