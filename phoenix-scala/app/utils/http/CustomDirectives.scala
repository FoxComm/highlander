package utils.http

import java.io.{ByteArrayInputStream, DataInputStream}

import scala.concurrent.Future
import scala.util.{Failure, Random, Success, Try}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher.Matched
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import cats.data.Xor
import failures._
import models.account._
import models.activity.ActivityContext
import models.objects.{ObjectContext, ObjectContexts}
import models.product.{ProductReference, SimpleContext}
import org.json4s.jackson.Serialization.{write ⇒ json}
import payloads.AuthPayload
import services.{JwtCookie, Result}
import slick.driver.PostgresDriver.api._
import utils._
import utils.aliases._
import utils.db._
import utils.http.Http._
import com.github.levkhomich.akka.tracing._
import com.github.levkhomich.akka.tracing.http.TracingHeaders._
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec

object CustomDirectives extends LazyLogging {

  private val DebugFlag = 1L

  final case class TracingRequest(override val spanName: String) extends TracingSupport

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

  def traceStart(service: String, trace: TracingExtensionImpl): Directive1[TracingRequest] = {
    extractRequest.map { request ⇒
      def headers(name: String): Option[String] = request.headers.find(_.name == name).map(_.value)

      val cr = TracingRequest("client")
      val tr = TracingRequest(request.uri.path.toString())

      extractSpan(headers) match {
        case Some(span) ⇒
          logger.info(s"Child trace. ${tr.spanName}")
          trace.importMetadata(cr, span, "client")
          trace.createChild(tr, cr).foreach { md ⇒
            trace.importMetadata(tr, md, service)
            trace.record(tr, TracingAnnotations.ServerReceived.text)
          }
        case None ⇒
          logger.info(s"New trace. ${tr.spanName}")
          trace.sample(tr, service, true)
      }

      trace.recordKeyValue(tr, "request.uri", request.uri.toString())
      trace.recordKeyValue(tr, "request.path", request.uri.path.toString())
      trace.recordKeyValue(tr, "request.method", request.method.value)
      request.uri.query().toMultiMap.foreach {
        case (key, values) ⇒
          values.foreach(trace.recordKeyValue(tr, "request.query." + key, _))
      }
      tr
    }
  }

  def traceEnd[T <: AnyRef](t: T)(implicit tr: TracingRequest, trace: TracingExtensionImpl): T = {
    trace.record(tr, TracingAnnotations.ServerSend.text)
    logger.info(s"In traceEnd. ${TracingAnnotations.ServerSend}")

    t
  }

  @tailrec
  private def idFromString(x: String): Option[Long] = {
    x match {
      case x if x == null || x.length == 0 || x.length > 32 ⇒ None
      case x if x.length > 16                               ⇒ idFromString(x.takeRight(16))
      case x ⇒ {
        val s = x match {
          case x if x.length % 2 == 0 ⇒ x
          case x ⇒ "0" + x
        }
        val bytes = new Array[Byte](8)
        val start = 7 - (s.length + 1) / 2
        (s.length until 0 by -2).foreach { i ⇒
          val x = Integer.parseInt(s.substring(i - 2, i), 16).toByte
          bytes.update(start + i / 2, x)
        }
        Some(new DataInputStream(new ByteArrayInputStream(bytes)).readLong)
      }
    }
  }

  private def extractSpan(headers: String ⇒ Option[String]): Option[SpanMetadata] = {
    def headerLongValue(name: String): Option[Long] = headers(name).flatMap(idFromString)

    def spanId: Long = headerLongValue(SpanId).getOrElse(Random.nextLong)

    // debug flag forces sampling (see http://git.io/hdEVug)
    val maybeForceSampling = headers(Sampled).map(_.toLowerCase) match {
      case Some("0") | Some("false") ⇒
        Some(false)
      case Some("1") | Some("true") ⇒
        Some(true)
      case _ ⇒
        headers(Flags).flatMap(flags ⇒
              Try((java.lang.Long.parseLong(flags) & DebugFlag) == DebugFlag).toOption.filter(v ⇒
                    v))
    }

    maybeForceSampling match {
      case Some(false) ⇒ None
      case _ ⇒
        val forceSampling = maybeForceSampling.getOrElse(false)
        headerLongValue(TraceId) match {
          case Some(traceId) ⇒
            val parentId = headerLongValue(ParentSpanId)
            Some(SpanMetadata(traceId, spanId, parentId, forceSampling))
          case _ ⇒
            None
        }
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

  def good[A <: AnyRef](a: Future[A])(implicit ec: EC,
                                      tr: TracingRequest,
                                      trace: TracingExtensionImpl): StandardRoute =
    complete(traceEnd(a.map(render(_))))

  def good[A <: AnyRef](a: A)(implicit tr: TracingRequest,
                              trace: TracingExtensionImpl): StandardRoute =
    complete(traceEnd(render(a)))

  private def renderGoodOrFailures[G <: AnyRef](or: Failures Xor G): HttpResponse =
    or.fold(renderFailure(_), render(_))

  def goodOrFailures[A <: AnyRef](a: Result[A])(implicit ec: EC,
                                                tr: TracingRequest,
                                                trace: TracingExtensionImpl): StandardRoute =
    complete(traceEnd(a.map(renderGoodOrFailures)))

  def getOrFailures[A <: AnyRef](a: DbResultT[A])(implicit ec: EC,
                                                  db: DB,
                                                  tr: TracingRequest,
                                                  trace: TracingExtensionImpl): StandardRoute =
    complete(traceEnd(a.run().map(renderGoodOrFailures)))

  def mutateOrFailures[A <: AnyRef](a: DbResultT[A])(implicit ec: EC,
                                                     db: DB,
                                                     tr: TracingRequest,
                                                     trace: TracingExtensionImpl): StandardRoute =
    complete(traceEnd(a.runTxn().map(renderGoodOrFailures)))

  def mutateWithNewTokenOrFailures[A <: AnyRef](a: DbResultT[(A, AuthPayload)])(
      implicit ec: EC,
      db: DB,
      tr: TracingRequest,
      trace: TracingExtensionImpl): Route = {
    onSuccess(a.runTxn()) { result ⇒
      result.fold({ f ⇒
        complete(renderFailure(f))
      }, { resp ⇒
        {
          val (body, auth) = resp
          respondWithHeader(RawHeader("JWT", auth.jwt)).&(setCookie(JwtCookie(auth))) {
            complete(
                HttpResponse(
                    entity = HttpEntity(ContentTypes.`application/json`, json(body))
                ))
          }
        }
      })
    }
  }

  def deleteOrFailures(a: DbResultT[_])(implicit ec: EC,
                                        db: DB,
                                        tr: TracingRequest,
                                        trace: TracingExtensionImpl): StandardRoute =
    complete(a.runTxn().map(_.fold(renderFailure(_), _ ⇒ noContentResponse)))

  def doOrFailures(a: DbResultT[_])(implicit ec: EC,
                                    db: DB,
                                    tr: TracingRequest,
                                    trace: TracingExtensionImpl): StandardRoute =
    complete(a.runTxn().map(_.fold(renderFailure(_), _ ⇒ noContentResponse)))

  def entityOr[T](um: FromRequestUnmarshaller[T], failure: failures.Failure)(
      implicit tr: TracingRequest,
      trace: TracingExtensionImpl): Directive1[T] =
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
