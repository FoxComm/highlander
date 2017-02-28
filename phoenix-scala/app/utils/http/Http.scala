package utils.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._

import com.typesafe.scalalogging.LazyLogging
import failures.{Failure, Failures, NotFoundFailure404}
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.json4s.{Formats, jackson}
import utils.Environment

object Http extends LazyLogging {
  import utils.JsonFormatters._

  implicit lazy val serialization: Serialization.type = jackson.Serialization
  implicit lazy val formats: Formats                  = phoenixFormats

  val notFoundResponse: HttpResponse   = HttpResponse(NotFound)
  val noContentResponse: HttpResponse  = HttpResponse(NoContent)
  val badRequestResponse: HttpResponse = HttpResponse(BadRequest)

  private def renderNotFoundFailure(f: NotFoundFailure404): HttpResponse =
    notFoundResponse.copy(entity = errorsJson(Seq(f)))

  def render[A <: AnyRef](resource: A, statusCode: StatusCode = OK) =
    HttpResponse(statusCode, entity = jsonEntity(resource))

  def renderPlain(text: String): HttpResponse =
    HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, text))

  def renderFailure(failures: Failures, statusCode: ClientError = BadRequest): HttpResponse = {
    val failuresList = failures.toList
    val notFound     = failuresList.collectFirst { case f: NotFoundFailure404 ⇒ f }
    notFound.fold(HttpResponse(statusCode, entity = errorsJson(failuresList))) { nf ⇒
      renderNotFoundFailure(nf)
    }
  }

  def jsonEntity[A <: AnyRef](resource: A): ResponseEntity =
    HttpEntity(ContentTypes.`application/json`, json(resource))

  def errorsJson(failures: Seq[Failure]): ResponseEntity = {
    val errorsJson = "errors" → failures.map(_.description)

    val allFailuresDebug = failures.map(_.debug)

    // Logging only at HTTP error response render so we don't get logs clogged with errors that were silenced
    // with `DbResultT#fold` or any other way
    if (allFailuresDebug.count(_.nonEmpty) > 0) logger.error(allFailuresDebug.mkString("\n"))

    if (Environment.default.isProd)
      jsonEntity(errorsJson)
    else {
      // Replace \n line breaks with JSON array ¯\_(ツ)_/¯
      val debugJson = "debug" → allFailuresDebug.map(_.map(_.split("\n").toList))
      // Append debug info to errors
      jsonEntity(errorsJson ~ debugJson)
    }
  }
}
