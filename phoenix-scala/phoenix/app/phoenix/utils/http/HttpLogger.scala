package phoenix.utils.http

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.scaladsl.Sink
import cats.implicits._
import org.json4s.JsonAST.{JField, JString}
import org.json4s._
import org.json4s.jackson.{parseJson, Serialization}
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._

import scala.concurrent.Future

object HttpLogger {

  implicit val formats = JsonFormatters.phoenixFormats

  private val fieldsToMaskCompletely = Seq("cvv")
  private val marker                 = "routes"
  private val errorLevel             = Logging.ErrorLevel

  def logFailedRequests(route: Route, logger: LoggingAdapter)(implicit mat: Mat, ec: EC) = {
    def loggingFn(logger: LoggingAdapter)(request: HttpRequest)(res: Any): Unit = {

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

  private def logError(request: HttpRequest, response: HttpResponse)(implicit mat: Mat,
                                                                     ec: EC): Future[Option[LogEntry]] =
    for {
      requestEntity  ← entityToString(request.entity)
      responseEntity ← entityToString(response.entity)
    } yield {
      val requestJson  = maskSensitiveData(parseJson(requestEntity))
      val responseJson = parseJson(responseEntity)
      LogEntry(
        s"""|${request.method.name} ${request.uri}: ${response.status}
            |Request entity:  ${Serialization.write(requestJson)}
            |Response entity: ${Serialization.write(responseJson)}""".stripMargin,
        marker,
        errorLevel
      ).some
    }

  private def maskSensitiveData(json: Json): Json = json.transformField {

    // Replace all symbols with "*"
    case JField(name, JString(value)) if fieldsToMaskCompletely.contains(name) ⇒
      (name, JString("*" * value.length))

    // Mask password with constant number of asterisks
    case JField("password", JString(password)) ⇒
      ("password", JString("*" * 3))

    // Mask everything but last four digits
    case JField("cardNumber", JString(cardNumber)) ⇒
      ("cardNumber", JString("*" * (cardNumber.length - 4) + cardNumber.takeRight(4)))
  }

  private def entityToString(entity: HttpEntity)(implicit m: Mat) =
    entity.dataBytes.map(_.decodeString("UTF-8")).runWith(Sink.head)
}
