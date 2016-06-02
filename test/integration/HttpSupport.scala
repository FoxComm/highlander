import java.net.ServerSocket

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.Await.result
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString

import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkasse.EventStreamUnmarshalling._
import de.heikoseeberger.akkasse.ServerSentEvent
import models.StoreAdmin
import models.customer.Customer
import org.json4s.Formats
import org.json4s.jackson.Serialization.{write ⇒ writeJson}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, MustMatchers, Suite, SuiteMixin}
import responses.TheResponse
import server.Service
import services.Authenticator
import services.Authenticator.AsyncAuthenticator
import util.DbTestSupport
import utils.aliases._
import utils.{Apis, FoxConfig, JsonFormatters, StripeApi}

// TODO: Move away from root package when `Service' moverd
object HttpSupport {
  @volatile var akkaConfigured = false

  protected var system: ActorSystem             = _
  protected var materializer: ActorMaterializer = _
  protected var service: Service                = _
  protected var serverBinding: ServerBinding    = _
}

trait HttpSupport
    extends SuiteMixin
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterAll
    with MockitoSugar {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒

  import HttpSupport._

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  private val ActorSystemNameChars =
    ('a' to 'z').toSet | ('A' to 'Z').toSet | ('0' to '9').toSet | Set('-', '_')

  private val ValidResponseContentTypes = Set(
      ContentTypes.`application/json`, ContentTypes.NoContentType)

  import Extensions._

  protected implicit lazy val mat: ActorMaterializer   = materializer
  protected implicit lazy val actorSystem: ActorSystem = system

  protected def additionalRoutes: immutable.Seq[Route] = immutable.Seq.empty

  override protected def beforeAll: Unit = {
    super.beforeAll
    if (!akkaConfigured) {
      system = ActorSystem("system", actorSystemConfig)
      materializer = ActorMaterializer()

      akkaConfigured = true
    }

    service = makeService

    serverBinding = service.bind(ConfigFactory.parseString(s"""
           |http.interface = 127.0.0.1
           |http.port      = ${getFreePort}
        """.stripMargin)).futureValue
  }

  override protected def afterAll: Unit = {
    super.afterAll
    Await.result(for {
      _ ← Http().shutdownAllConnectionPools()
      _ ← service.close()
    } yield {}, 1.minute)
  }

  private def actorSystemConfig =
    ConfigFactory.parseString("""
      |akka {
      |  log-dead-letters = off
      |}
    """.stripMargin).withFallback(ConfigFactory.load())

  def makeApis: Option[Apis] = Some(Apis(mock[StripeApi]))

  def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] =
    Authenticator.BasicStoreAdmin()

  def overrideCustomerAuth: AsyncAuthenticator[Customer] =
    Authenticator.BasicCustomer()

  implicit val env = FoxConfig.Test

  private def makeService: Service =
    new Service(dbOverride = Some(db),
                systemOverride = Some(system),
                apisOverride = makeApis,
                addRoutes = additionalRoutes) {

      override val storeAdminAuth: AsyncAuthenticator[StoreAdmin] = overrideStoreAdminAuth

      override val customerAuth: AsyncAuthenticator[Customer] = overrideCustomerAuth
    }

  def POST(path: String, rawBody: String): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.POST,
                              uri = pathToAbsoluteUrl(path),
                              entity = HttpEntity.Strict(
                                  ContentTypes.`application/json`,
                                  ByteString(rawBody)
                              ))

    dispatchRequest(request)
  }

  def POST(path: String): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.POST, uri = pathToAbsoluteUrl(path))

    dispatchRequest(request)
  }

  def PATCH(path: String, rawBody: String): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.PATCH,
                              uri = pathToAbsoluteUrl(path),
                              entity = HttpEntity.Strict(
                                  ContentTypes.`application/json`,
                                  ByteString(rawBody)
                              ))

    dispatchRequest(request)
  }

  def PATCH(path: String): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.PATCH, uri = pathToAbsoluteUrl(path))

    dispatchRequest(request)
  }

  def GET(path: String): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.GET, uri = pathToAbsoluteUrl(path))

    dispatchRequest(request)
  }

  def POST[T <: AnyRef](path: String, payload: T): HttpResponse =
    POST(path, writeJson(payload))

  def PATCH[T <: AnyRef](path: String, payload: T): HttpResponse =
    PATCH(path, writeJson(payload))

  def DELETE(path: String): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.DELETE, uri = pathToAbsoluteUrl(path))

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

  def parseErrors(response: HttpResponse)(implicit ec: EC): List[String] =
    response.errors

  private def dispatchRequest(req: HttpRequest): HttpResponse = {
    val response = Http().singleRequest(req, settings = connectionPoolSettings).futureValue
    ValidResponseContentTypes must contain(response.entity.contentType)
    response
  }

  lazy final val connectionPoolSettings = ConnectionPoolSettings
    .default(implicitly[ActorSystem])
    .withMaxConnections(32)
    .withMaxOpenRequests(32)
    .withMaxRetries(0)

  object SSE {

    def sseProbe(path: String, skipHeartbeat: Boolean = true): Probe[String] =
      probe(if (skipHeartbeat) skipHeartbeats(sseSource(path))
          else sseSource(path))

    def sseSource(path: String): Source[String, Any] = {
      val localAddress = serverBinding.localAddress

      Source
        .single(Get(pathToAbsoluteUrl(path)))
        .via(Http().outgoingConnection(localAddress.getHostString, localAddress.getPort))
        .mapAsync(1)(Unmarshal(_).to[Source[ServerSentEvent, Any]])
        .runWith(Sink.head)
        .futureValue
        .map(_.data)
    }

    def skipHeartbeats(sse: Source[String, Any]): Source[String, Any] =
      sse.via(Flow[String].filter(_.nonEmpty))

    def probe(source: Source[String, Any]) =
      source.runWith(TestSink.probe[String])
  }
}

object Extensions {
  implicit class RichHttpResponse(val res: HttpResponse) extends AnyVal {
    import org.json4s.jackson.JsonMethods._

    def bodyText(implicit ec: EC, mat: Mat): String =
      result(res.entity.toStrict(1.second).map(_.data.utf8String), 1.second)

    def as[A <: AnyRef](implicit fm: Formats, mf: Manifest[A], mat: Mat): A =
      parse(bodyText).extract[A]

    def ignoreFailuresAndGiveMe[A <: AnyRef](implicit fm: Formats,
                                             mf: Manifest[A],
                                             mat: Mat): A =
      parse(bodyText).extract[TheResponse[A]].result

    def withResultTypeOf[A <: AnyRef](implicit fm: Formats,
                                      mf: Manifest[A],
                                      mat: Mat): TheResponse[A] =
      parse(bodyText).extract[TheResponse[A]]

    def errors(implicit fm: Formats, mat: Mat): List[String] =
      (parse(bodyText) \ "errors").extractOrElse[List[String]](List.empty[String])

    def error(implicit fm: Formats, mat: Mat): String =
      errors.headOption.getOrElse("never gonna give you up. never gonna let you down.")
  }
}
