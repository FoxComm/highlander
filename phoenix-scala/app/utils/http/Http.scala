package utils.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, ResponseEntity, StatusCode, StatusCodes}
import failures.{Failures, NotFoundFailure404}
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.json4s.{Formats, jackson}
import responses.{BatchMetadata, TheResponse}
import utils.db.MetaResponse

object Http {
  import utils.JsonFormatters._

  implicit lazy val serialization: Serialization.type = jackson.Serialization
  implicit lazy val formats: Formats                  = phoenixFormats

  val notFoundResponse: HttpResponse   = HttpResponse(NotFound)
  val noContentResponse: HttpResponse  = HttpResponse(NoContent)
  val badRequestResponse: HttpResponse = HttpResponse(BadRequest)

  private def renderNotFoundFailure(f: NotFoundFailure404): HttpResponse =
    notFoundResponse.copy(entity = jsonEntity("errors" → Seq(f.message)))

  private final case class SuccessfulResponse(result: Any,
                                              warnings: Option[List[String]],
                                              errors: Option[List[String]],
                                              batch: Option[BatchMetadata])

  private object SuccessfulResponse {
    def from(result: Any, uiInfo: List[MetaResponse]): SuccessfulResponse = {
      val uiInfoWarnings = uiInfo.collect { case MetaResponse.Warning(f)        ⇒ f.description }
      val uiInfoErrors   = uiInfo.collect { case MetaResponse.Error(f)          ⇒ f.description }
      val uiInfoBatches  = uiInfo.collectFirst { case MetaResponse.BatchInfo(b) ⇒ b }
      // FIXME: have a way of merging multiple `BatchMetadata`s, as types allow for that. @michalrus
      def emptyToNoneNonemptyToSome[A](xs: List[A]): Option[List[A]] =
        if (xs.nonEmpty) Some(xs) else None
      result match {
        // FIXME: get rid of `TheResponse` and s/AnyRef/Any/ around here. @michalrus
        case TheResponse(res, alerts, errors, warnings, batch) ⇒
          SuccessfulResponse(
              result = res,
              warnings = emptyToNoneNonemptyToSome(
                  uiInfoWarnings ::: alerts.toList.flatten ::: warnings.toList.flatten),
              errors = emptyToNoneNonemptyToSome(uiInfoErrors ::: errors.toList.flatten),
              batch = uiInfoBatches orElse batch)
        case raw ⇒
          SuccessfulResponse(result = raw,
                             warnings = emptyToNoneNonemptyToSome(uiInfoWarnings),
                             errors = emptyToNoneNonemptyToSome(uiInfoErrors),
                             batch = uiInfoBatches)
      }
    }
  }

  def renderRaw(resource: AnyRef) = // TODO: is this needed anymore? @michalrus
    HttpResponse(OK, entity = jsonEntity(resource))

  def render(result: AnyRef, uiInfo: List[MetaResponse], statusCode: StatusCode = OK) = {
    val response = SuccessfulResponse.from(result, uiInfo)
    val temporaryHack: AnyRef = result match {
      case _: TheResponse[_] ⇒ response
      case _ if response.batch.isEmpty && response.warnings.isEmpty ⇒
        response.result
          .asInstanceOf[AnyRef] // Includes autoboxing for AnyVals. This is only temporary! @michalrus
      case _ ⇒ response
    }
    HttpResponse(statusCode, entity = jsonEntity(temporaryHack))
  }

  def renderPlain(text: String): HttpResponse =
    HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, text))

  def renderFailure(failures: Failures, statusCode: ClientError = BadRequest): HttpResponse = {
    val failuresList = failures.toList
    val notFound     = failuresList.collectFirst { case f: NotFoundFailure404 ⇒ f }
    notFound.fold(HttpResponse(statusCode,
                               entity = jsonEntity("errors" → failuresList.map(_.description)))) {
      nf ⇒
        renderNotFoundFailure(nf)
    }
  }

  private def jsonEntity[A <: AnyRef](resource: A): ResponseEntity =
    HttpEntity(ContentTypes.`application/json`, json(resource))
}
