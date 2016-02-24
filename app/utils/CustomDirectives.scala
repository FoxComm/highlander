package utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MalformedRequestContentRejection, UnsupportedRequestContentTypeRejection,
RequestEntityExpectedRejection, Directive1, StandardRoute, ValidationRejection}
import akka.http.scaladsl.unmarshalling.{Unmarshaller, FromRequestUnmarshaller}
import slick.driver.PostgresDriver.api._

import services.Result
import utils.Http._

import models.{Customer, StoreAdmin}
import models.product.{SimpleContext, ProductContext, ProductContexts}
import models.activity.ActivityContext

object CustomDirectives {

  val DefaultPageSize = 50
  val DefaultContextName = SimpleContext.name

  final case class Sort(sortColumn: String, asc: Boolean = true)
  final case class SortAndPage(
    from: Option[Int] = Some(0),
    size: Option[Int] = Some(DefaultPageSize),
    sortBy: Option[String]) {

    require(from.getOrElse(1) >= 0, "from parameter must be non-negative")
    require(size.getOrElse(1) >  0, "size parameter must be positive")

    def sort: Option[Sort] = sortBy.map { f ⇒
      if (f.startsWith("-")) Sort(f.drop(1), asc = false)
      else Sort(f)
    }
  }


  val EmptySortAndPage: SortAndPage = SortAndPage(None, None, None)

  def activityContext(admin: StoreAdmin) : Directive1[ActivityContext] = {
    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒  
        ActivityContext(userId = admin.id, userType = "admin", transactionId = uuid)
      case (None) ⇒
        ActivityContext(userId = admin.id, userType = "admin", transactionId = generateUuid)
    }
  }

  def activityContext(customer: Customer) : Directive1[ActivityContext] = {
    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒
        ActivityContext(userId = customer.id, userType = "customer", transactionId = uuid)
      case (None) ⇒
        ActivityContext(userId = customer.id, userType = "customer", transactionId = generateUuid)
    }
  }

  def activityContext() : Directive1[ActivityContext] = {
    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒
        ActivityContext(userId = 0, userType = "guest", transactionId = uuid)
      case (None) ⇒
        ActivityContext(userId = 0, userType = "guest", transactionId = generateUuid)
    }
  }

  /**
   * At the moment we support one context. The input to this function will
   * and it will become a combination of of things which will then search
   * for the correct context.
   */
  def determineProductContext(implicit db: Database, ec: ExecutionContext) : Directive1[ProductContext] = {
    onSuccess(db.run(ProductContexts.filterByName(DefaultContextName).result.headOption).map {
      case Some(c) ⇒  c
      case None ⇒ throw new Exception("Unable to find default context. Is the DB seeded?")
    })
  }


  def sortAndPage: Directive1[SortAndPage] =
    parameters(('from.as[Int].?, 'size.as[Int].?, 'sortBy.as[String].?)).as(SortAndPage)

  def good[A <: AnyRef](a: Future[A])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(render(_)))

  def good[A <: AnyRef](a: A): StandardRoute =
    complete(render(a))

  def goodOrNotFound[A <: AnyRef](a: Future[Option[A]])(implicit ec: ExecutionContext): StandardRoute =
    complete(renderOrNotFound(a))

  def goodOrFailures[A <: AnyRef](a: Result[A])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(renderGoodOrFailures))

  def nothingOrFailures(a: Result[_])(implicit ec: ExecutionContext): StandardRoute =
    complete(a.map(renderNothingOrFailures))

  def entityOr[T](um: FromRequestUnmarshaller[T], failure: services.Failure): Directive1[T] =
    extractRequestContext.flatMap[Tuple1[T]] { ctx ⇒
      import ctx.executionContext
      import ctx.materializer
      onComplete(um(ctx.request)).flatMap {
        case Success(value) ⇒
          provide(value)
        case Failure(Unmarshaller.NoContentException) ⇒
          reject(RequestEntityExpectedRejection)
        case Failure(Unmarshaller.UnsupportedContentTypeException(x)) ⇒
          reject(UnsupportedRequestContentTypeRejection(x))
        case Failure(x: Throwable) ⇒
          ctx.log.error("Error unmarshalling request {} body: {}", ctx.request, failure.description)
          reject(ValidationRejection(s"${failure.description}", None))
        case Failure(x) ⇒
          ctx.log.error("Error unmarshalling request {} body: {}", ctx.request, failure.description)
          reject(MalformedRequestContentRejection(s"${failure.description}", None))
      }
    } & cancelRejections(RequestEntityExpectedRejection.getClass, classOf[UnsupportedRequestContentTypeRejection])
}
