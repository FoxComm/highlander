package utils

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.{HttpResponse, StatusCode}
import akka.http.scaladsl.model.StatusCodes._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.scalactic.{Bad, Good, Or}
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import services.Failure

object Http {
  import utils.JsonFormatters._

  implicit val serialization = jackson.Serialization
  implicit val formats = phoenixFormats

  val notFoundResponse = HttpResponse(NotFound)

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.AsInstanceOf"))
  def renderGoodOrBad[G <: AnyRef, B <: AnyRef](goodOrBad: G Or B)
    (implicit ec: ExecutionContext, db: Database): HttpResponse = {
    goodOrBad match {
      case Bad(errors)    ⇒
        errors match {
          case _: Iterable[_] ⇒
            render("errors" → errors.asInstanceOf[Iterable[Failure]].flatMap(_.description), BadRequest)
          case _: Failure ⇒
            render("errors" → errors.asInstanceOf[Failure].description, BadRequest)
          case _ ⇒
            render("errors" → errors, BadRequest)
        }
      case Good(resource) ⇒
        render(resource)
    }
  }

  def whenFound[A, G <: AnyRef, B <: AnyRef](finder: Future[Option[A]])(f: A => Future[G Or B])
    (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {
    finder.flatMap { option =>
      option.map(f(_).map(renderGoodOrBad)).
        getOrElse(Future.successful(notFoundResponse))
    }
  }

  def renderOrNotFound[A <: AnyRef](resource: Future[Option[A]],
    onFound: (A => HttpResponse) = (r: A) => render(r))(implicit ec: ExecutionContext) = {
    resource.map {
      case Some(r) => onFound(r)
      case None => notFoundResponse
    }
  }

  def render[A <: AnyRef](resource: A, statusCode: StatusCode = OK) =
    HttpResponse(statusCode, entity = json(resource))
}
