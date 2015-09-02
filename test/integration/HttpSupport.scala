import java.net.ServerSocket

import scala.concurrent.Await.result
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString

import com.typesafe.config.ConfigFactory
import org.json4s.{Formats, DefaultFormats}
import org.json4s.jackson.Serialization.{write ⇒ writeJson}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{Outcome, Suite, SuiteMixin}
import util.DbTestSupport
import utils.JsonFormatters
import concurrent.ExecutionContext.Implicits.global

import cats.std.future._
import cats.syntax.flatMap._

// TODO: Move away from root package when `Service' moverd
trait HttpSupport extends SuiteMixin with ScalaFutures { this: Suite with PatienceConfiguration with DbTestSupport ⇒

  implicit val formats = JsonFormatters.phoenixFormats

  private val ActorSystemNameChars = ('a' to 'z').toSet | ('A' to 'Z').toSet | ('0' to '9').toSet | Set('-', '_')

  import org.json4s.jackson.JsonMethods._
  import Extensions._

  /* State shared that is set / reset in withFixture subtypes */
  protected implicit var as: ActorSystem       = _
  protected implicit var fm: ActorMaterializer = _

  /** of the currenly running server */
  protected var serverBinding: ServerBinding = _

  override abstract protected def withFixture(test: NoArgTest): Outcome = {
    as = ActorSystem(test.name.filter(ActorSystemNameChars.contains), actorSystemConfig)
    fm = ActorMaterializer()
    val service = makeService

    try {
      serverBinding = service.bind(ConfigFactory.parseString(
        s"""
           |http.interface = 127.0.0.1
           |http.port      = ${ getFreePort }
        """.stripMargin)).futureValue

      super.withFixture(test)
    } finally {
      (Http().shutdownAllConnectionPools() >> service.close()).futureValue

      as.shutdown()
      as.awaitTermination()
    }
  }

  private def actorSystemConfig = ConfigFactory.parseString(
    """
      |akka {
      |  log-dead-letters = off
      |}
    """.stripMargin).withFallback(ConfigFactory.load)

  def makeService: Service = new Service(dbOverride = Some(db), systemOverride = Some(as))

  def POST(path: String, rawBody: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = pathToAbsoluteUrl(path),
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(rawBody)
      ))

    Http().singleRequest(request).futureValue
  }

  def POST(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = pathToAbsoluteUrl(path))

    Http().singleRequest(request).futureValue
  }

  def PATCH(path: String, rawBody: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.PATCH,
      uri    = pathToAbsoluteUrl(path),
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(rawBody)
      ))

    Http().singleRequest(request).futureValue
  }

  def PATCH(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.PATCH,
      uri    = pathToAbsoluteUrl(path))

    Http().singleRequest(request).futureValue
  }

  def GET(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri    = pathToAbsoluteUrl(path))

    Http().singleRequest(request).futureValue
  }

  def POST[T <: AnyRef](path: String, payload: T): HttpResponse = POST(path, writeJson(payload))

  def PATCH[T <: AnyRef](path: String, payload: T): HttpResponse = PATCH(path, writeJson(payload))

  def DELETE(path: String): HttpResponse = {
    val request = HttpRequest(
      method = HttpMethods.DELETE,
      uri    = pathToAbsoluteUrl(path))

    Http().singleRequest(request).futureValue
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
}

object Extensions {
  implicit class RichHttpResponse(val res: HttpResponse) extends AnyVal {
    import org.json4s.jackson.JsonMethods._

    def bodyText(implicit ec: ExecutionContext, mat: Materializer): String =
      result(res.entity.toStrict(1.second).map(_.data.utf8String), 1.second)

    def as[A <: AnyRef](implicit fm: Formats, mf: scala.reflect.Manifest[A], mat: Materializer): A =
      parse(bodyText).extract[A]

    def errors(implicit fm: Formats, mat: Materializer): List[String] =
      parse(bodyText).extract[Map[String, List[String]]].getOrElse("errors", List.empty[String])
  }
}
