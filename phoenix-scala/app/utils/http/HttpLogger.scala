package utils.http

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.scaladsl.Sink
import cats.implicits._
import io.circe.Json
import io.circe.jackson.syntax._
import io.circe.parser.parse
import scala.concurrent.Future
import utils.aliases._
import utils.json._

object HttpLogger {
  private val fieldsToMaskCompletely = Set("cvv")
  private val marker                 = "routes"
  private val errorLevel             = Logging.ErrorLevel

  def logFailedRequests(route: Route, logger: LoggingAdapter)(implicit mat: Mat, ec: EC): Route = {
    def loggingFn(logger: LoggingAdapter)(request: HttpRequest)(res: RouteResult): Unit = {

      val entry: Future[Option[LogEntry]] = res match {
        case Complete(response) ⇒
          response.status match {
            case StatusCodes.OK ⇒
              Future.successful(None)
            case _ ⇒
              logError(request, response)
          }
        case Rejected(rejections) ⇒
          Future.successful(LogEntry(s"${rejections.mkString(", ")}", marker, errorLevel).some)
      }
      entry.foreach(_.foreach(_.logTo(logger)))
    }
    DebuggingDirectives.logRequestResult(LoggingMagnet(_ ⇒ loggingFn(logger)))(route)
  }

  private def logError(request: HttpRequest, response: HttpResponse)(
      implicit mat: Mat,
      ec: EC): Future[Option[LogEntry]] = {
    for {
      requestEntity  ← entityToString(request.entity)
      responseEntity ← entityToString(response.entity)
    } yield {
      val requestJson = parse(requestEntity).map(maskSensitiveData).getOrElse(Json.obj())
      LogEntry(s"""|${request.method.name} ${request.uri}: ${response.status}
            |Request entity:  ${requestJson.jacksonPrint}
            |Response entity: $responseEntity""".stripMargin,
               marker,
               errorLevel).some
    }
  }

  private def maskSensitiveData(json: Json): Json = json.transformField {
    // Replace all symbols with "*"
    case (name, value) if fieldsToMaskCompletely.contains(name) ⇒
      (name, value.mapString(s ⇒ "*" * s.length))

    // Mask password with constant number of asterisks
    case ("password", password) ⇒
      ("password", password.mapString(_ ⇒ "*" * 3))

    // Mask everything but last four digits
    case ("cardNumber", cardNumber) ⇒
      ("cardNumber", cardNumber.mapString(cn ⇒ "*" * (cn.length - 4) + cn.takeRight(4)))
  }

  private def entityToString(entity: HttpEntity)(implicit m: Mat) =
    entity.dataBytes.map(_.decodeString("UTF-8")).runWith(Sink.head)
}
