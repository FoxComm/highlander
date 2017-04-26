package utils.http

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher.Matched
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures._
import io.circe.Encoder
import models.activity.ActivityContext
import models.objects.{ObjectContext, ObjectContexts}
import models.product.{ProductReference, SimpleContext}
import payloads.AuthPayload
import scala.concurrent.Future
import scala.util.{Failure, Success}
import services.JwtCookie
import slick.driver.PostgresDriver.api._
import utils._
import utils.aliases._
import utils.db._
import utils.http.Http._

object CustomDirectives {

  val DefaultContextName = SimpleContext.default

  object ProductRef extends PathMatcher1[ProductReference] {
    def apply(path: Path) = {
      path match {
        case Path.Segment(segment, tail) if segment.exists(_.isLetter) ⇒
          Matched(tail, Tuple1(ProductReference(segment)))
        case _                                        ⇒
          IntNumber.apply(path).map { case Tuple1(id) ⇒ Tuple1(ProductReference(id)) }
      }
    }
  }

  def activityContext(au: AU): Directive1[ActivityContext] = {
    val user  = au.model
    val scope = LTree(au.token.scope)

    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒
        ActivityContext(userId = user.accountId,
                        userType = "user",
                        transactionId = uuid,
                        scope = scope)
      case (None) ⇒
        ActivityContext(userId = user.accountId,
                        userType = "user",
                        transactionId = generateUuid,
                        scope = scope)
    }
  }

  def activityContext(scope: LTree): Directive1[ActivityContext] = {
    optionalHeaderValueByName("x-request-id").map {
      case (Some(uuid)) ⇒
        ActivityContext(userId = 0, userType = "guest", transactionId = uuid, scope = scope)
      case (None) ⇒
        ActivityContext(userId = 0,
                        userType = "guest",
                        transactionId = generateUuid,
                        scope = scope)
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
      case Success(Right(ctx)) ⇒
        route(ctx)
      case Success(Left(msg)) ⇒
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
      case Some(c) ⇒ Either.right(c)
      case None    ⇒ Either.left(s"Context with name $name cannot be found")
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

  def good[A: Encoder](a: Future[A])(implicit ec: EC): StandardRoute = // TODO: is this needed anymore? @michalrus
    complete(a.map(renderRaw(_)))

  def good[A: Encoder](a: A): StandardRoute = // TODO: is this needed anymore? @michalrus
    complete(renderRaw(a))

  def goodOrFailures[A: Encoder](a: Result[A])(implicit ec: EC): StandardRoute =
    complete(
        a.runEmpty.value.map(_.fold(renderFailure(_), { case (uiInfo, a) ⇒ render(a, uiInfo) })))

  def getOrFailures[A: Encoder](a: DbResultT[A])(implicit ec: EC, db: DB): StandardRoute =
    goodOrFailures(a.runDBIO())

  def mutateOrFailures[A: Encoder](a: DbResultT[A])(implicit ec: EC, db: DB): StandardRoute =
    goodOrFailures(a.runTxn())

  def mutateWithNewTokenOrFailures[A: Encoder](a: DbResultT[(A, AuthPayload)])(implicit ec: EC,
                                                                               db: DB): Route = {
    onSuccess(a.runTxn().runEmpty.value) { result ⇒
      result.fold(f ⇒ complete(renderFailure(f)), { resp ⇒
        val (uiInfo, (body, auth)) = resp
        respondWithHeader(RawHeader("JWT", auth.jwt)).&(setCookie(JwtCookie(auth))) {
          complete(render(body, uiInfo))
        }
      })
    }
  }

  def deleteOrFailures(a: DbResultT[_])(implicit ec: EC, db: DB): StandardRoute =
    doOrFailures(a)

  def doOrFailures(a: DbResultT[_])(implicit ec: EC, db: DB): StandardRoute = // TODO: rethink discarding warnings here @michalrus
    complete(a.runTxn().runEmptyA.value.map(_.fold(renderFailure(_), _ ⇒ noContentResponse)))

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
