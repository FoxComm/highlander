package utils

import scala.collection.immutable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, RejectionHandler}
import Directives._
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.json4s.jackson.Serialization.{write ⇒ json}
import utils.Http._

object SprayHandlers {

  private val defaultRejectionHandler = RejectionHandler.default

  def jsonRejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
    case rejection if defaultRejectionHandler(immutable.Seq(rejection)).isDefined =>
      mapResponseEntity { entity ⇒
        entity.withContentType(ContentTypes.`application/json`).transformDataBytes {
          Flow[ByteString].map { chunk =>
            val str = chunk.utf8String
            ByteString(json("errors" → Seq(chunk.utf8String)))
          }
        }
      }(defaultRejectionHandler(immutable.Seq(rejection))
        .getOrError("defaultRejectionHandler(immutable.Seq(rejection)) should be defined"))
  }
    .handleNotFound {
    complete(HttpResponse(NotFound, entity = HttpEntity(ContentTypes.`application/json`,
      json("errors" → Seq("The requested resource could not be found.")))))
  }
    .result()
}
