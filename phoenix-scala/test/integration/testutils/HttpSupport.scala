package testutils

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model._
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
import java.net.ServerSocket
import org.json4s.Formats
import org.json4s.jackson.Serialization.{write ⇒ writeJson}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import server.Service
import services.Authenticator.UserAuthenticator
import utils.FoxConfig.config
import utils.apis.Apis
import utils.seeds.Factories
import utils.{FoxConfig, JsonFormatters}

object HttpSupport {
  implicit lazy val system: ActorSystem =
    ActorSystem("phoenix-integration-tests", actorSystemConfig)
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  private def actorSystemConfig =
    ConfigFactory.parseString("""
                                |akka {
                                |  log-dead-letters = off
                                |}
                              """.stripMargin).withFallback(ConfigFactory.load())
}

trait HttpSupport
    extends SuiteMixin
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterAll
    with TestObjectContext {
  self: FoxSuite ⇒

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  private val validResponseContentTypes =
    Set(ContentTypes.`application/json`, ContentTypes.NoContentType)

  protected implicit def mat: ActorMaterializer   = HttpSupport.materializer
  protected implicit def actorSystem: ActorSystem = HttpSupport.system

  private[this] var service: Service             = _
  private[this] var serverBinding: ServerBinding = _

  protected def additionalRoutes: immutable.Seq[Route] = immutable.Seq.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    // init
    HttpSupport.system
    HttpSupport.materializer

    service = makeService

    serverBinding = service
      .bind(
          FoxConfig.http.modify(config)(_.copy(
                  interface = "127.0.0.1",
                  port = getFreePort
              )))
      .futureValue
  }

  override protected def afterAll(): Unit = {
    super.afterAll()

    for {
      _ ← Http().shutdownAllConnectionPools()
      _ ← service.close()
    } yield ()
  }

  val adminUser    = Factories.storeAdmin.copy(id = 1, accountId = 1)
  val customerData = Factories.customer.copy(id = 2, accountId = 2)

  def overrideUserAuth: UserAuthenticator =
    AuthAs(adminUser, customerData)

  def apisOverride: Apis

  private def makeService: Service =
    new Service(dbOverride = Some(db),
                systemOverride = Some(actorSystem),
                apisOverride = Some(apisOverride),
                addRoutes = additionalRoutes) {

      override val userAuth: UserAuthenticator = overrideUserAuth
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

  protected def dispatchRequest(req: HttpRequest): HttpResponse = {
    val response = Http().singleRequest(req, settings = connectionPoolSettings).futureValue
    validResponseContentTypes must contain(response.entity.contentType)
    response
  }

  lazy final val connectionPoolSettings: ConnectionPoolSettings = ConnectionPoolSettings
    .default(implicitly[ActorSystem])
    .withMaxConnections(32)
    .withMaxOpenRequests(32)
    .withMaxRetries(0)

  object SSE {

    def sseProbe(path: String, skipHeartbeat: Boolean = true): Probe[String] =
      probe(
          if (skipHeartbeat) skipHeartbeatsAndAdminCreated(sseSource(path))
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

    def skipHeartbeatsAndAdminCreated(sse: Source[String, Any]): Source[String, Any] =
      sse.via(Flow[String].filter(n ⇒ n.nonEmpty && !n.contains("store_admin_created")))

    def probe(source: Source[String, Any]): Probe[String] =
      source.runWith(TestSink.probe[String])
  }
}
