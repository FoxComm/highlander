package foxcomm.agni.api

import com.sksamuel.elastic4s.ElasticImplicits._
import com.twitter.finagle.Http
import com.twitter.finagle.http.Status
import com.twitter.util.Await
import foxcomm.agni._
import foxcomm.utils.finch._
import io.circe.generic.extras.auto._
import io.finch._
import io.finch.circe._
import org.elasticsearch.common.ValidationException
import scala.concurrent.ExecutionContext

object Api extends App {
  def endpoints(searchService: SearchService)(implicit ec: ExecutionContext) =
    post(
      "search" :: string :: string :: param("size")
        .as[Int] :: paramOption("from").as[Int] :: jsonBody[SearchPayload]) {
      (searchIndex: String, searchType: String, size: Int, from: Option[Int], searchQuery: SearchPayload) ⇒
        searchService
          .searchFor(searchIndex / searchType, searchQuery, searchSize = size, searchFrom = from)
          .toTwitterFuture
          .map(Ok)
    } :+: get("ping") {
      Ok("pong")
    }

  def errorHandler[A]: PartialFunction[Throwable, Output[A]] = {
    case ex: ValidationException ⇒ Output.failure(ex, Status.BadRequest)
    case ex: Exception           ⇒ Output.failure(ex, Status.InternalServerError)
    case ex                      ⇒ Output.failure(new RuntimeException(ex), Status.InternalServerError)
  }

  implicit val ec: ExecutionContext = ExecutionContext.global
  val config                        = AppConfig.load()
  val svc                           = SearchService.fromConfig(config)

  Await.result(
    Http.server
      .withStreaming(enabled = true)
      .serve(s"${config.http.interface}:${config.http.port}",
             endpoints(svc).handle(errorHandler).toServiceAs[Application.Json]))
}
