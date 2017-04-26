package utils.http

import akka.http.scaladsl.unmarshalling._
import cats.data.Validated.{Invalid, Valid}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import failures._
import io.circe.Decoder
import scala.concurrent.Future
import utils._

case class FoxValidationException(failures: Failures) extends Exception

object JsonSupport extends CirceSupport {
  private def validateData[A <: Validation[_]](data: A): Future[A] = data.validate match {
    case Valid(_)          ⇒ Future.successful(data)
    case Invalid(failures) ⇒ Future.failed(FoxValidationException(failures))
  }

  implicit def circeValidationUnmarshaller[A <: Validation[_]: Decoder]
    : FromEntityUnmarshaller[A] = {
    circeUnmarshaller[A].flatMap(implicit ec ⇒ mat ⇒ validateData)
  }
}
