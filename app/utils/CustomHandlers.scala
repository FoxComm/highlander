package utils

import scala.collection.immutable
import scala.compat.Platform._
import scala.util.control.NonFatal
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{ExceptionHandler, Directives, RejectionHandler}
import Directives._
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.json4s.jackson.Serialization.{write ⇒ json}
import utils.Http._

object CustomHandlers {

  private val defaultRejectionHandler = RejectionHandler.default

  private val isProduction = Config.environment == Config.Production

  private def errorsJson(msg: String): String = json("errors" → Seq(msg))

  private def errorsJsonEntity(msg: String): ResponseEntity = HttpEntity(ContentTypes.`application/json`,
    errorsJson(msg))

  def jsonRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
    case rejection if defaultRejectionHandler(immutable.Seq(rejection)).isDefined =>
      mapResponseEntity { entity ⇒
        entity.withContentType(ContentTypes.`application/json`).transformDataBytes {
          Flow[ByteString].map { chunk =>
            ByteString(errorsJson(chunk.utf8String))
          }
        }
      }(defaultRejectionHandler(immutable.Seq(rejection))
        .getOrError("defaultRejectionHandler(immutable.Seq(rejection)) should be defined"))
  }
    .handleNotFound {
    complete(HttpResponse(NotFound, entity = errorsJsonEntity("The requested resource could not be found.")))
  }
    .result()

  def jsonExceptionHandler: ExceptionHandler = ExceptionHandler {
    case IllegalRequestException(info, status) ⇒ ctx ⇒ {
      ctx.log.warning(s"Illegal request {}\n\t{}\n\tCompleting with '{}' response",
        ctx.request, info.formatPretty, status)
      ctx.complete(HttpResponse(status, entity = errorsJsonEntity(info.format(isProduction))))
    }
    case NonFatal(e) ⇒ ctx ⇒ {
      val errMsg = if(isProduction)
        "There was an internal server error."
      else
        e.getMessage + e.getStackTrace.mkString(":" + EOL, EOL, EOL)
      ctx.log.error(e, "Error during processing of request {}", ctx.request)
      ctx.complete(HttpResponse(InternalServerError, entity = errorsJsonEntity(errMsg)))
    }
  }
}
