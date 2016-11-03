package utils.http

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}

import cats.data.Xor
import failures._
import models.auth.UserToken
import models.account._
import models.activity.ActivityContext
import models.objects.{ObjectContext, ObjectContexts}
import models.product.SimpleContext
import services.Result
import slick.driver.PostgresDriver.api._
import utils._
import utils.aliases._
import utils.db._
import utils.http.Http._

object CustomDirectives {

  val DefaultContextName = SimpleContext.default

  def activityContext(user: User): Directive1[ActivityContext] = {
    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒
        ActivityContext(userId = user.accountId, userType = "user", transactionId = uuid)
      case (None) ⇒
        ActivityContext(userId = user.accountId, userType = "user", transactionId = generateUuid)
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

  def adminObjectContext(contextName: String)(route: ObjectContext ⇒ Route)(implicit db: DB,
                                                                            ec: EC): Route =
    onComplete(tryGetContextByName(contextName)) {
      case Success(Xor.Right(ctx)) ⇒
        route(ctx)
      case Success(Xor.Left(msg)) ⇒
        complete(renderFailure(NotFoundFailure404(msg).single, StatusCodes.NotFound))
      case Failure(ex) ⇒
        complete(renderFailure(GeneralFailure(ex.getMessage()).single))
    }

  private def getContextByName(name: String)(implicit db: DB, ec: EC) =
    db.run(ObjectContexts.filterByName(name).result.headOption).map {
      case Some(c) ⇒ c
      case None    ⇒ throw new Exception(s"Unable to find context $name.")
    }

  private def tryGetContextByName(name: String)(implicit db: DB, ec: EC) =
    db.run(ObjectContexts.filterByName(name).result.headOption).map {
      case Some(c) ⇒ Xor.Right(c)
      case None    ⇒ Xor.Left(s"Context with name $name cannot be found")
    }

  //This is a really trivial version. We are not handling language weights,
  //and multiple options.
  private def getContextByLanguage(lang: String)(implicit db: DB, ec: EC) =
    db.run(ObjectContexts.filterByLanguage(lang).result.headOption).flatMap {
      case Some(c) ⇒
        Future {
          c
        }
      case None ⇒ getContextByName(DefaultContextName)
    }

  def good[A <: AnyRef](a: Future[A])(implicit ec: EC): StandardRoute =
    complete(a.map(render(_)))

  def good[A <: AnyRef](a: A): StandardRoute =
    complete(render(a))

  private def renderGoodOrFailures[G <: AnyRef](or: Failures Xor G): HttpResponse =
    or.fold(renderFailure(_), render(_))

  def goodOrFailures[A <: AnyRef](a: Result[A])(implicit ec: EC): StandardRoute =
    complete(a.map(renderGoodOrFailures))

  def getOrFailures[A <: AnyRef](a: DbResultT[A])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.run().map(renderGoodOrFailures))

  def mutateOrFailures[A <: AnyRef](a: DbResultT[A])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.runTxn().map(renderGoodOrFailures))

  def deleteOrFailures(a: DbResultT[_])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.runTxn().map(_.fold(renderFailure(_), _ ⇒ noContentResponse)))

  def doOrFailures(a: DbResultT[_])(implicit ec: EC, db: DB): StandardRoute =
    complete(a.runTxn().map(_.fold(renderFailure(_), _ ⇒ noContentResponse)))

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
    } & cancelRejections(RequestEntityExpectedRejection.getClass,
                         classOf[UnsupportedRequestContentTypeRejection])
}
