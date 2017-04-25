package testutils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Cookie
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
import io.circe.Encoder
import io.circe.jackson.syntax._
import io.circe.syntax._
import java.net.ServerSocket
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import server.Service
import utils.FoxConfig
import utils.FoxConfig.config
import utils.apis.Apis

// TODO: Move away from root package when `Service' moverd
object HttpSupport {
  @volatile var akkaConfigured = false

  protected var system: ActorSystem             = _
  protected var materializer: ActorMaterializer = _
  protected var service: Service                = _
  var serverBinding: ServerBinding              = _
}

trait HttpSupport
    extends SuiteMixin
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterAll
    with TestObjectContext {
  self: FoxSuite ⇒

  import HttpSupport._

  private val validResponseContentTypes =
    Set(ContentTypes.`application/json`, ContentTypes.NoContentType)

  protected implicit lazy val mat: ActorMaterializer   = materializer
  protected implicit lazy val actorSystem: ActorSystem = system

  protected def additionalRoutes: immutable.Seq[Route] = immutable.Seq.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    if (!akkaConfigured) {
      system = ActorSystem("system", actorSystemConfig)
      materializer = ActorMaterializer()

      akkaConfigured = true
    }

    service = makeService

    serverBinding = service
      .bind(
          FoxConfig.http.modify(config)(_.copy(
                  interface = "127.0.0.1",
                  port = getFreePort
              )))
      .futureValue
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

  def apisOverride: Option[Apis]

  private def makeService: Service =
    new Service(dbOverride = Some(db),
                systemOverride = Some(system),
                apisOverride = apisOverride,
                addRoutes = additionalRoutes) {}

  def POST(path: String, rawBody: String, jwtCookie: Option[Cookie]): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.POST,
                              uri = pathToAbsoluteUrl(path),
                              entity = HttpEntity.Strict(
                                  ContentTypes.`application/json`,
                                  ByteString(rawBody)
                              ))

    dispatchRequest(request, jwtCookie)
  }

  def POST(path: String, jwtCookie: Option[Cookie]): HttpResponse =
    dispatchRequest(HttpRequest(method = HttpMethods.POST, uri = pathToAbsoluteUrl(path)),
                    jwtCookie)

  def PATCH(path: String, rawBody: String, jwtCookie: Option[Cookie]): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.PATCH,
                              uri = pathToAbsoluteUrl(path),
                              entity = HttpEntity.Strict(
                                  ContentTypes.`application/json`,
                                  ByteString(rawBody)
                              ))

    dispatchRequest(request, jwtCookie)
  }

  def PATCH(path: String, jwtCookie: Option[Cookie]): HttpResponse =
    dispatchRequest(HttpRequest(method = HttpMethods.PATCH, uri = pathToAbsoluteUrl(path)),
                    jwtCookie)

  def GET(path: String, jwtCookie: Option[Cookie]): HttpResponse =
    dispatchRequest(HttpRequest(method = HttpMethods.GET, uri = pathToAbsoluteUrl(path)),
                    jwtCookie)

  def POST[T: Encoder](path: String, payload: T, jwtCookie: Option[Cookie]): HttpResponse =
    POST(path, payload.asJson.jacksonPrint, jwtCookie)

  def PATCH[T: Encoder](path: String, payload: T, jwtCookie: Option[Cookie]): HttpResponse =
    PATCH(path, payload.asJson.jacksonPrint, jwtCookie)

  def DELETE(path: String, jwtCookie: Option[Cookie]): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.DELETE, uri = pathToAbsoluteUrl(path))

    dispatchRequest(request, jwtCookie)
  }

  def pathToAbsoluteUrl(path: String): Uri = {
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

  protected def dispatchRequest(req: HttpRequest, jwtCookie: Option[Cookie]): HttpResponse = {
    val withCookie = jwtCookie.fold(req)(req.addHeader(_))
    val response   = Http().singleRequest(withCookie, settings = connectionPoolSettings).futureValue
    validResponseContentTypes must contain(response.entity.contentType)
    response
  }

  lazy final val connectionPoolSettings: ConnectionPoolSettings = ConnectionPoolSettings
    .default(implicitly[ActorSystem])
    .withMaxConnections(32)
    .withMaxOpenRequests(32)
    .withMaxRetries(0)

  object SSE {

    def sseProbe(path: String, jwtCookie: Cookie, skipHeartbeat: Boolean = true): Probe[String] =
      probe(
          if (skipHeartbeat) skipHeartbeatsAndAdminCreated(sseSource(path, jwtCookie))
          else sseSource(path, jwtCookie))

    def sseSource(path: String, jwtCookie: Cookie): Source[String, Any] = {
      val localAddress = serverBinding.localAddress

      Source
        .single(Get(pathToAbsoluteUrl(path)).addHeader(jwtCookie))
        .via(Http().outgoingConnection(localAddress.getHostString, localAddress.getPort))
        .mapAsync(1)(Unmarshal(_).to[Source[ServerSentEvent, Any]])
        .runWith(Sink.head)
        .futureValue
        .map(_.data)
    }

    def skipHeartbeatsAndAdminCreated(sse: Source[String, Any]): Source[String, Any] =
      sse.via(Flow[String].filter(n ⇒ n.nonEmpty && !n.contains("store_admin_created")))

    def probe(source: Source[String, Any]): Probe[String] =
      source.runWith(TestSink.probe[String])
  }
}
