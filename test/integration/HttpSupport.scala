import java.net.ServerSocket

import scala.collection.immutable
import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.http.ConnectionPoolSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString

import com.typesafe.config.ConfigFactory
import models.{Customer, StoreAdmin}
import org.json4s.Formats
import org.json4s.jackson.Serialization.{write ⇒ writeJson}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{MustMatchers, Args, Status, Suite, SuiteMixin}
import responses.TheResponse
import server.Service
import services.Authenticator
import util.DbTestSupport
import utils.{StripeApi, Apis, JsonFormatters}
import concurrent.ExecutionContext.Implicits.global

import cats.std.future._
import cats.syntax.flatMap._

// TODO: Move away from root package when `Service' moverd
trait HttpSupport
  extends SuiteMixin
  with ScalaFutures
  with MustMatchers
  with MockitoSugar { this: Suite with PatienceConfiguration with DbTestSupport ⇒

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  private val ActorSystemNameChars = ('a' to 'z').toSet | ('A' to 'Z').toSet | ('0' to '9').toSet | Set('-', '_')

  private val ValidResponseContentTypes = Set(ContentTypes.`application/json`, ContentTypes.NoContentType)

  import org.json4s.jackson.JsonMethods._
  import Extensions._

  protected implicit var system:        ActorSystem       = _
  protected implicit var materializer:  ActorMaterializer = _
  protected          var service:       Service           = _
  protected          var serverBinding: ServerBinding     = _

  protected def additionalRoutes: immutable.Seq[Route] = immutable.Seq.empty

  override protected abstract def runTests(testName: Option[String], args: Args): Status = {
    system       = ActorSystem("system", actorSystemConfig)
    materializer = ActorMaterializer()
    service      = makeService

    serverBinding = service.bind(ConfigFactory.parseString(
      s"""
         |http.interface = 127.0.0.1
         |http.port      = ${ getFreePort }
      """.stripMargin)).futureValue

    try super.runTests(testName, args)

    finally {
      (Http().shutdownAllConnectionPools() >> service.close()).futureValue

      system.shutdown()
      system.awaitTermination()
    }
  }

  private def actorSystemConfig = ConfigFactory.parseString(
    """
      |akka {
      |  log-dead-letters = off
      |}
    """.stripMargin).withFallback(ConfigFactory.load)

  def makeApis: Option[Apis] = Some(Apis(mock[StripeApi]))

  def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = Authenticator.storeAdmin

  def overrideCustomerAuth: AsyncAuthenticator[Customer] = Authenticator.customer

  private def makeService: Service = new Service(
    dbOverride = Some(db),
    systemOverride = Some(system),
    apisOverride = makeApis,
    addRoutes = additionalRoutes) {

    override def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = overrideStoreAdminAuth

    override def customerAuth: AsyncAuthenticator[Customer] = overrideCustomerAuth
  }

  def POST(path: String, rawBody: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = pathToAbsoluteUrl(path),
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(rawBody)
      ))

    dispatchRequest(request)
  }

  def POST(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = pathToAbsoluteUrl(path))

    dispatchRequest(request)
  }

  def PATCH(path: String, rawBody: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.PATCH,
      uri    = pathToAbsoluteUrl(path),
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(rawBody)
      ))

    dispatchRequest(request)
  }


  def PATCH(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.PATCH,
      uri    = pathToAbsoluteUrl(path))

    dispatchRequest(request)
  }

  def GET(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri    = pathToAbsoluteUrl(path))

    dispatchRequest(request)
  }

  def POST[T <: AnyRef](path: String, payload: T): HttpResponse = POST(path, writeJson(payload))

  def PATCH[T <: AnyRef](path: String, payload: T): HttpResponse = PATCH(path, writeJson(payload))

  def DELETE(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.DELETE,
      uri    = pathToAbsoluteUrl(path))

    dispatchRequest(request)
  }

  def pathToAbsoluteUrl(path: String) = {
    val host = serverBinding.localAddress.getHostString
    val port = serverBinding.localAddress.getPort

    Uri(s"http://$host:$port/$path")
  }

  /**
   * Returns an ephemeral port that is free, that is not currently used by the
   * OS and not likely to be used.
   *
   * Ephemeral ports are part of a pool that is managed by the kernel.
   * We get a port and immediately release it.
   *
   * Those ports are assigned in ascending order from the kernel until it wraps over,
   * so this must be quite safe.
   */
  def getFreePort: Int = {
    val socket = new ServerSocket(0)
    val port   = socket.getLocalPort
    socket.close()

    port
  }

  def parseErrors(response: HttpResponse)(implicit ec: ExecutionContext): List[String] =
    response.errors

  private def dispatchRequest(req: HttpRequest): HttpResponse = {
    val response = Http().singleRequest(req, connectionPoolSettings).futureValue
    ValidResponseContentTypes must contain(response.entity.contentType())
    response
  }

  lazy final val connectionPoolSettings = ConnectionPoolSettings.create(implicitly[ActorSystem]).copy(
    maxConnections  = 32,
    maxOpenRequests = 32,
    maxRetries      = 0)
}

object Extensions {
  implicit class RichHttpResponse(val res: HttpResponse) extends AnyVal {
    import org.json4s.jackson.JsonMethods._

    def bodyText(implicit ec: ExecutionContext, mat: Materializer): String =
      result(res.entity.toStrict(1.second).map(_.data.utf8String), 1.second)

    def as[A <: AnyRef](implicit fm: Formats, mf: scala.reflect.Manifest[A], mat: Materializer): A =
      parse(bodyText).extract[A]

    def ignoreFailuresAndGiveMe[A <: AnyRef](implicit fm: Formats, mf: scala.reflect.Manifest[A], mat: Materializer): A =
      parse(bodyText).extract[TheResponse[A]].result

    def withResultTypeOf[A <: AnyRef](implicit fm: Formats, mf: scala.reflect.Manifest[A], mat: Materializer): TheResponse[A] =
      parse(bodyText).extract[TheResponse[A]]

    def errors(implicit fm: Formats, mat: Materializer): List[String] =
      parse(bodyText).extract[Map[String, List[String]]].getOrElse("errors", List.empty[String])
  }
}
