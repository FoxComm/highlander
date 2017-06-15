package testutils

import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.NotUsed
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
import de.heikoseeberger.akkasse.scaladsl.unmarshalling.EventStreamUnmarshalling._
import de.heikoseeberger.akkasse.scaladsl.model.ServerSentEvent
import org.json4s.Formats
import org.json4s.jackson.Serialization.{write ⇒ writeJson}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import phoenix.server.Service
import phoenix.utils.FoxConfig.config
import phoenix.utils.apis.Apis
import phoenix.utils.{FoxConfig, JsonFormatters}

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

// TODO: Move away from root package when `Service' moverd
object HttpSupport {

  implicit lazy val system: ActorSystem =
    ActorSystem("phoenix-integration-tests", actorSystemConfig)

  private def actorSystemConfig =
    ConfigFactory.parseString("""
                                 |akka {
                                 |  log-dead-letters = off
                                 |}
                               """.stripMargin).withFallback(ConfigFactory.load())

  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

}

trait HttpSupport
    extends SuiteMixin
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterAll
    with TestObjectContext { self: FoxSuite ⇒

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  private val validResponseContentTypes =
    Set(ContentTypes.`application/json`, ContentTypes.NoContentType)

  protected implicit def mat: ActorMaterializer   = HttpSupport.materializer
  protected implicit def actorSystem: ActorSystem = HttpSupport.system

  private[this] lazy val service: Service = makeService
  private[this] lazy val serverBinding: ServerBinding = {
    service
      .bind(
        FoxConfig.http.modify(config)(
          _.copy(
            interface = "127.0.0.1",
            port = getFreePort
          )))
      .futureValue
  }

  protected def additionalRoutes: immutable.Seq[Route] = immutable.Seq.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    // init
    HttpSupport.system
    HttpSupport.materializer
    serverBinding
  }

  override protected def afterAll: Unit = {
    super.afterAll()
    for {
      _ ← service.close()
    } yield ()
  }

  def apisOverride: Option[Apis]

  private def makeService: Service =
    new Service(dbOverride = Some(db),
                systemOverride = Some(actorSystem),
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

  def PUT(path: String, rawBody: String, jwtCookie: Option[Cookie]): HttpResponse = {
    val request = HttpRequest(method = HttpMethods.PUT,
                              uri = pathToAbsoluteUrl(path),
                              entity = HttpEntity.Strict(
                                ContentTypes.`application/json`,
                                ByteString(rawBody)
                              ))

    dispatchRequest(request, jwtCookie)
  }

  def POST(path: String, jwtCookie: Option[Cookie]): HttpResponse =
    dispatchRequest(HttpRequest(method = HttpMethods.POST, uri = pathToAbsoluteUrl(path)), jwtCookie)

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
    dispatchRequest(HttpRequest(method = HttpMethods.PATCH, uri = pathToAbsoluteUrl(path)), jwtCookie)

  def GET(path: String, jwtCookie: Option[Cookie]): HttpResponse =
    dispatchRequest(HttpRequest(method = HttpMethods.GET, uri = pathToAbsoluteUrl(path)), jwtCookie)

  def POST[T <: AnyRef](path: String, payload: T, jwtCookie: Option[Cookie]): HttpResponse =
    POST(path, writeJson(payload), jwtCookie)

  def PUT[T <: AnyRef](path: String, payload: T, jwtCookie: Option[Cookie]): HttpResponse =
    PUT(path, writeJson(payload), jwtCookie)

  def PATCH[T <: AnyRef](path: String, payload: T, jwtCookie: Option[Cookie]): HttpResponse =
    PATCH(path, writeJson(payload), jwtCookie)

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

    def sseSource(path: String, jwtCookie: Cookie): Source[String, NotUsed] = {
      val localAddress = serverBinding.localAddress

      Source
        .single(Get(pathToAbsoluteUrl(path)).addHeader(jwtCookie))
        .via(Http().outgoingConnection(localAddress.getHostString, localAddress.getPort))
        .mapAsync(1)(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
        .runWith(Sink.head)
        .futureValue
        .map(_.data)
    }

    def skipHeartbeatsAndAdminCreated(sse: Source[String, NotUsed]): Source[String, NotUsed] =
      sse.via(Flow[String].filter(n ⇒ n.nonEmpty && !n.contains("store_admin_created")))

    def probe(source: Source[String, NotUsed]): Probe[String] =
      source.runWith(TestSink.probe[String])
  }
}
