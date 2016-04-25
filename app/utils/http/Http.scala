package utils.http

import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, ResponseEntity, StatusCode}
import cats.implicits._
import cats.data.Xor
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.json4s.{Formats, jackson}
import failures.{Failures, NotFoundFailure404}
import utils.aliases._

object Http {
  import utils.JsonFormatters._

  implicit lazy val serialization: Serialization.type = jackson.Serialization
  implicit lazy val formats:       Formats = phoenixFormats

  val notFoundResponse:   HttpResponse  = HttpResponse(NotFound)
  val noContentResponse:  HttpResponse  = HttpResponse(NoContent)
  val badRequestResponse: HttpResponse  = HttpResponse(BadRequest)

  def renderGoodOrFailures[G <: AnyRef](or: Failures Xor G): HttpResponse =
    or.fold(renderFailure(_), render(_))

  def renderNothingOrFailures(or: Failures Xor _): HttpResponse =
    or.fold(renderFailure(_), _ ⇒ noContentResponse)

  def renderOrNotFound[A <: AnyRef](resource: Future[Option[A]],
    onFound: (A ⇒ HttpResponse) = (r: A) ⇒ render(r))(implicit ec: EC) = {
    resource.map {
      case Some(r) ⇒ onFound(r)
      case None ⇒ notFoundResponse
    }
  }

  def renderOrNotFound[A <: AnyRef](resource: Option[A]): HttpResponse =
    resource.fold(notFoundResponse)(render(_))

  def renderOrBadRequest[A <: AnyRef](resource: Option[A]): HttpResponse =
    resource.fold(badRequestResponse)(render(_))

  def renderNotFoundFailure(f: NotFoundFailure404): HttpResponse =
    notFoundResponse.copy(entity = jsonEntity("errors" → Seq(f.message)))

  def render[A <: AnyRef](resource: A, statusCode: StatusCode = OK) =
    HttpResponse(statusCode, entity = jsonEntity(resource))

  def renderFailure(failures: Failures, statusCode: ClientError = BadRequest): HttpResponse = {
    val failuresList = failures.unwrap
    val notFound = failuresList.collectFirst { case f: NotFoundFailure404 ⇒ f }
    notFound.fold(HttpResponse(statusCode, entity = jsonEntity("errors" → failuresList.map(_.description)))) { nf ⇒
      renderNotFoundFailure(nf)
    }
  }

  def jsonEntity[A <: AnyRef](resource: A): ResponseEntity = HttpEntity(ContentTypes.`application/json`,
    json(resource))
}
