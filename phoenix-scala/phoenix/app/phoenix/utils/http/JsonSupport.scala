package phoenix.utils.http

import akka.http.scaladsl.unmarshalling._
import cats.data.Validated.{Invalid, Valid}
import core.utils.Validation
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.heikoseeberger.akkahttpjson4s.Json4sSupport.{unmarshaller ⇒ json4sUnmarshaller}
import core.failures._
import org.json4s.{Formats, Serialization}

import scala.concurrent.Future

case class FoxValidationException(failures: Failures) extends Exception

object JsonSupport extends Json4sSupport {
  private def validateData[A <: Validation[_]](data: A): Future[A] = data.validate match {
    case Valid(_)          ⇒ Future.successful(data)
    case Invalid(failures) ⇒ Future.failed(FoxValidationException(failures))
  }

  implicit def json4sValidationUnmarshaller[A <: Validation[_]: Manifest](
      implicit serialization: Serialization,
      formats: Formats): FromEntityUnmarshaller[A] =
    json4sUnmarshaller[A].flatMap(implicit ec ⇒ mat ⇒ validateData)
}
