package utils.http

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}

import models.StoreAdmin
import models.activity.ActivityContext
import models.customer.Customer
import models.objects.{ObjectContext, ObjectContexts}
import models.product.SimpleContext
import services.{Result, ResultT}
import slick.driver.PostgresDriver.api._
import utils.http.Http._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._
import utils._

object CustomDirectives {

  val DefaultPageSize    = 50
  val DefaultContextName = SimpleContext.default

  case class Sort(sortColumn: String, asc: Boolean = true)
  case class SortAndPage(from: Option[Int] = Some(0),
                         size: Option[Int] = Some(DefaultPageSize),
                         sortBy: Option[String]) {

    require(from.getOrElse(1) >= 0, "from parameter must be non-negative")
    require(size.getOrElse(1) > 0, "size parameter must be positive")

    def sort: Option[Sort] = sortBy.map { f ⇒
      if (f.startsWith("-")) Sort(f.drop(1), asc = false)
      else Sort(f)
    }
  }

  val EmptySortAndPage: SortAndPage = SortAndPage(None, None, None)

  def activityContext(admin: StoreAdmin): Directive1[ActivityContext] = {
    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒
        ActivityContext(userId = admin.id, userType = "admin", transactionId = uuid)
      case (None) ⇒
        ActivityContext(userId = admin.id, userType = "admin", transactionId = generateUuid)
    }
  }

  def activityContext(customer: Customer): Directive1[ActivityContext] = {
    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒
        ActivityContext(userId = customer.id, userType = "customer", transactionId = uuid)
      case (None) ⇒
        ActivityContext(userId = customer.id, userType = "customer", transactionId = generateUuid)
    }
  }

  def activityContext(): Directive1[ActivityContext] = {
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
  def determineObjectContext(implicit db: DB, ec: EC): Directive1[ObjectContext] = {
    optionalHeaderValueByName("Accept-Language").flatMap {
      case Some(lang) ⇒ onSuccess(getContextByLanguage(lang))
      case None       ⇒ onSuccess(getContextByName(DefaultContextName))
    }
  }

  def adminObjectContext(name: String)(implicit db: DB, ec: EC): Directive1[ObjectContext] =
    onSuccess(getContextByName(name))

  private def getContextByName(name: String)(implicit db: DB, ec: EC) =
    db.run(ObjectContexts.filterByName(name).result.headOption).map {
      case Some(c) ⇒ c
      case None    ⇒ throw new Exception(s"Unable to find context $name.")
    }

  //This is a really trivial version. We are not handling language weights, 
  //and multiple options.
  private def getContextByLanguage(lang: String)(implicit db: DB, ec: EC) =
    db.run(ObjectContexts.filterByLanguage(lang).result.headOption).flatMap {
      case Some(c) ⇒ Future { c }
      case None    ⇒ getContextByName(DefaultContextName)
    }

  def sortAndPage: Directive1[SortAndPage] =
    parameters(('from.as[Int].?, 'size.as[Int].?, 'sortBy.as[String].?)).as(SortAndPage)

  def good[A <: AnyRef](a: Future[A])(implicit ec: EC): StandardRoute =
    complete(a.map(render(_)))

  def good[A <: AnyRef](a: A): StandardRoute =
    complete(render(a))

  def goodOrFailures[A <: AnyRef](a: Result[A])(implicit ec: EC): StandardRoute =
    complete(a.map(renderGoodOrFailures))

  def getGoodOrFailures[A <: AnyRef](a: DbResultT[A])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.run().map(renderGoodOrFailures))

  def mutateGoodOrFailures[A <: AnyRef](a: DbResultT[A])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.runTxn().map(renderGoodOrFailures))

  def nothingOrFailures(a: Result[_])(implicit ec: EC): StandardRoute =
    complete(a.map(renderNothingOrFailures))

  def getNothingOrFailures(a: DbResultT[_])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.run().map(renderNothingOrFailures))

  def mutateNothingOrFailures(a: DbResultT[_])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.runTxn().map(renderNothingOrFailures))

  def entityOr[T](um: FromRequestUnmarshaller[T], failure: failures.Failure): Directive1[T] =
    extractRequestContext.flatMap[Tuple1[T]] { ctx ⇒
      import ctx.{executionContext, materializer}
      onComplete(um(ctx.request)).flatMap {
        case Success(value) ⇒
          provide(value)
        case Failure(Unmarshaller.NoContentException) ⇒
          reject(RequestEntityExpectedRejection)
        case Failure(Unmarshaller.UnsupportedContentTypeException(x)) ⇒
          reject(UnsupportedRequestContentTypeRejection(x))
        case Failure(x: Throwable) ⇒
          ctx.log.error("Error unmarshalling request {} body: {}",
                        ctx.request,
                        failure.description)
          reject(MalformedRequestContentRejection(s"${failure.description}", x))
      }
    } & cancelRejections(
        RequestEntityExpectedRejection.getClass, classOf[UnsupportedRequestContentTypeRejection])
}
