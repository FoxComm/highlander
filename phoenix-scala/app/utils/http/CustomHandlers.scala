package utils.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import io.circe.Json
import io.circe.jackson.syntax._
import scala.collection.immutable
import scala.concurrent.ExecutionException
import scala.util.control.NonFatal
import utils._
import utils.db.FoxFailureException

object CustomHandlers {

  private val defaultRejectionHandler = RejectionHandler.default

  private def errorsJson(msg: String): String =
    Json.obj("errors" → Json.arr(Json.fromString(msg))).jacksonPrint

  private def errorsJsonEntity(msg: String): ResponseEntity =
    HttpEntity(ContentTypes.`application/json`, errorsJson(msg))

  def jsonRejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case _ @MalformedRequestContentRejection(_, FoxValidationException(failures)) ⇒
          complete(Http.renderFailure(failures))

        case rejection if defaultRejectionHandler(immutable.Seq(rejection)).isDefined ⇒
          mapResponseEntity { entity ⇒
            entity.withContentType(ContentTypes.`application/json`).transformDataBytes {
              Flow[ByteString].map { chunk ⇒
                ByteString(errorsJson(chunk.utf8String))
              }
            }
          }(defaultRejectionHandler(immutable.Seq(rejection)).getOrError(
                  "defaultRejectionHandler(immutable.Seq(rejection)) should be defined"))
      }
      .handleNotFound {
        complete(
            HttpResponse(NotFound,
                         entity = errorsJsonEntity("The requested resource could not be found.")))
      }
      .result()

  def jsonExceptionHandler(implicit env: Environment): ExceptionHandler = {

    val baseHandler = ExceptionHandler {
      case IllegalRequestException(info, status) ⇒
        ctx ⇒
          {
            ctx.log.warning("Illegal request {}\n\t{}\n\tCompleting with '{}' response",
                            ctx.request,
                            info.formatPretty,
                            status)
            ctx.complete(HttpResponse(status, entity = errorsJsonEntity(info.format(env.isProd))))
          }

        case e: IllegalArgumentException ⇒
        ctx ⇒
          {
            ctx.log.warning("Bad request: {}", ctx.request)
            ctx.complete(HttpResponse(BadRequest, entity = errorsJsonEntity(e.getMessage)))
          }
        // This is not a part of our control flow, but I'll leave it here just in case of unanticipated DBIO.failed
        case FoxFailureException(failures) ⇒
        ctx ⇒
          ctx.complete(Http.renderFailure(failures))
        case NonFatal(e) ⇒
        ctx ⇒
          {
            val errMsg = if (env.isProd) "There was an internal server error." else e.getMessage
            ctx.log.warning("Error {} during processing of request {}", e, ctx.request)
            ctx.complete(HttpResponse(InternalServerError, entity = errorsJsonEntity(errMsg)))
          }
    }

    ExceptionHandler {
      case e: ExecutionException if e.getCause ne null ⇒
        baseHandler(e.getCause)
    } orElse baseHandler
  }
}
