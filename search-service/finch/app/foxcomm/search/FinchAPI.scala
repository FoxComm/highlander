package foxcomm.search

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.IndexAndTypes
import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.Http
import com.twitter.util.Await
import io.circe.generic.auto._
import io.finch._
import io.finch.circe._
import monix.execution.Scheduler

object FinchAPI {
  def search(searchService: SearchService)(
      implicit actorSystem: ActorSystem,
      scheduler: Scheduler): Endpoint[AsyncStream[String]] =
    post("search" :: path[IndexAndTypes] :: jsonBody[SearchQuery]) {
      (searchIndex: IndexAndTypes, searchQuery: SearchQuery) =>
        Ok(
          searchService
            .query(searchIndex, searchQuery)
            .toAsyncStream
            .map(_.getSourceAsString))
    }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("FinchAPI")
    implicit val scheduler: Scheduler = Scheduler(system.dispatcher)

    val config = AppConfig.load()
    val svc = SearchService.fromConfig(config)

    Await.result(
      Http.server
        .withStreaming(enabled = true)
        .serve(s":${config.http.port}",
               search(svc).toServiceAs[Application.Json]))
  }
}
