package foxcomm.agni.api

import com.twitter.finagle.Http
import com.twitter.finagle.http.Status
import com.twitter.util.Await
import foxcomm.agni._
import foxcomm.agni.interpreter.es.queryInterpreter
import foxcomm.utils.finch._
import io.circe.generic.extras.auto._
import io.finch._
import io.finch.circe._
import monix.execution.Scheduler
import org.elasticsearch.common.ValidationException

object Api extends App {
  def endpoints(searchService: SearchService)(implicit s: Scheduler) =
    post(
      "api" :: "search" :: string :: string :: param("size")
        .as[Int] :: paramOption("from").as[Int] :: jsonBody[SearchPayload]) {
      (searchIndex: String, searchType: String, size: Int, from: Option[Int], searchQuery: SearchPayload) ⇒
        searchService
          .searchFor(searchIndex = searchIndex,
                     searchType = searchType,
                     searchQuery = searchQuery,
                     searchSize = size,
                     searchFrom = from)
          .map(Ok)
          .toTwitterFuture
    } :+: get("ping") {
      Ok("pong")
    }

  def errorHandler[A]: PartialFunction[Throwable, Output[A]] = {
    case ex: ValidationException ⇒ Output.failure(ex, Status.BadRequest)
    case ex: Exception           ⇒ Output.failure(ex, Status.InternalServerError)
    case ex                      ⇒ Output.failure(new RuntimeException(ex), Status.InternalServerError)
  }

  implicit val s: Scheduler = Scheduler.global
  val config                = AppConfig.load()
  val svc                   = SearchService.fromConfig(config, queryInterpreter)

  Await.result(
    Http.server
      .withStreaming(enabled = true)
      .serve(s"${config.http.interface}:${config.http.port}",
             endpoints(svc).handle(errorHandler).toServiceAs[Application.Json]))
}
