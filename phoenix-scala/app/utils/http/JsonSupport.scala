package utils.http

import akka.http.scaladsl.unmarshalling._
import cats.data.Validated.{Invalid, Valid}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{Formats, Serialization}
import scala.concurrent.Future
import utils.{FoxValidationException, Validation}

object JsonSupport extends Json4sSupport {
  private def validateData[A <: Validation[A]](data: A): Future[A] = data.validate match {
    case Valid(v)          ⇒ Future.successful(v)
    case Invalid(failures) ⇒ Future.failed(FoxValidationException(failures))
  }

  implicit def json4sValidationUnmarshaller[A <: Validation[A]: Manifest](
      implicit serialization: Serialization,
      formats: Formats): FromEntityUnmarshaller[A] = {
    json4sUnmarshaller[A].flatMap(implicit ec ⇒ mat ⇒ validateData)
  }
}
