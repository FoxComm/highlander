package utils

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpResponse, StatusCode}

import cats.std.future.futureInstance
import org.json4s.jackson
import org.json4s.jackson.Serialization.{write ⇒ json}
import org.scalactic.{Bad, Good, Or}
import services.{Failure, Failures}
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object Http {
  import utils.JsonFormatters._

  implicit val serialization = jackson.Serialization
  implicit val formats = phoenixFormats

  val notFoundResponse = HttpResponse(NotFound)

  @SuppressWarnings(Array("org.brianmckenna.wartremover.warts.AsInstanceOf"))
  private[Http] def renderGoodOrBad[G <: AnyRef, B <: AnyRef](goodOrBad: G Or B)
    (implicit ec: ExecutionContext): HttpResponse = {
    goodOrBad match {
      case Bad(errors)    ⇒
        errors match {
          case _: Iterable[_] ⇒
            renderFailure(errors.asInstanceOf[Iterable[Failure]])
          case _: Failure ⇒
            renderFailure(Seq(errors.asInstanceOf[Failure]))
          case _ ⇒
            render("errors" → errors, BadRequest)
        }
      case Good(resource) ⇒
        render(resource)
    }
  }

  def renderGoodOrFailures[G <: AnyRef](that: G Or Failures)
                                       (implicit ec: ExecutionContext): HttpResponse =
    that.fold(render(_), renderFailure(_)) // Can’t pass eta expanded method because of by-name parameters

  def whenFound[A, G <: AnyRef, B <: AnyRef](finder: Future[Option[A]])(f: A => Future[G Or B])
    (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] = {
    finder.flatMap { option =>
      option.map(f(_).map(renderGoodOrBad)).
        getOrElse(Future.successful(notFoundResponse))
    }
  }

  // Todo: naming
  def whenFoundDispatchToService[A, G <: AnyRef](finder: Future[Option[A]])
    (f: A ⇒ Future[G Or Failures])
    (implicit ec: ExecutionContext, db: Database): Future[HttpResponse] =
    finder.flatMap { option: Option[A] ⇒
      option.map(f(_).map(renderGoodOrFailures)).getOrElse(Future.successful(notFoundResponse)) }

  def renderOrNotFound[A <: AnyRef](resource: Future[Option[A]],
    onFound: (A => HttpResponse) = (r: A) => render(r))(implicit ec: ExecutionContext) = {
    resource.map {
      case Some(r) => onFound(r)
      case None => notFoundResponse
    }
  }

  def render[A <: AnyRef](resource: A, statusCode: StatusCode = OK) =
    HttpResponse(statusCode, entity = json(resource))

  def renderFailure(failures: Traversable[Failure], statusCode: ClientError = BadRequest): HttpResponse =
    HttpResponse(statusCode, entity = json("errors" → failures.flatMap(_.description)))
}
