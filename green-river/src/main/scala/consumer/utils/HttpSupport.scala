package consumer.utils

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString

import consumer.aliases._
import cats.implicits._
import cats.data.{Xor, XorT}
import consumer.failures._
import scalacache._
import memoization._

final case class PhoenixConnectionInfo(uri: String, user: String, pass: String, org: String)

object HttpSupport {
  type HttpResult = XorT[Future, Failures, HttpResponse]

  object HttpResult {
    def right(v: Future[HttpResponse])(implicit ec: EC): HttpResult =
      XorT.right[Future, Failures, HttpResponse](v)
  }
}

import HttpSupport._

case class Phoenix(conn: PhoenixConnectionInfo)(implicit ec: EC, ac: AS, mat: AM, cp: CP, sc: SC) {

  val authUri = fullUri("public/login")

  val authBodyTemplate = """{"email": "%s", "password": "%s", "org": "%s"}"""

  val jwtHeaderName = "JWT"

  val authHeaderName = "JWT"

  val cache = typed[String, NoSerialization]

  def get(suffix: String): HttpResult =
    for {
      jwtToken ← getJwtToken
      response ← getRequest(suffix, jwtToken)
    } yield response

  def post(suffix: String, body: String): HttpResult =
    for {
      jwtToken ← getJwtToken
      response ← postRequest(suffix, body, jwtToken)
    } yield response

  private def getJwtToken: XorT[Future, Failures, String] = {
    cache.sync
      .get("jwtAuth")
      .fold {
        for {
          authResponse ← authenticate()
          jwtToken     ← XorT.fromXor[Future](extractJwtToken(authResponse))
          _            ← XorT.right[Future, Failures, Unit](cache.put("jwtAuth")(jwtToken, Some(7.days)))
        } yield jwtToken
      }(XorT.pure[Future, Failures, String])
  }

  private def authenticate(): HttpResult = {
    val request = HttpRequest(method = HttpMethods.POST,
                              uri = authUri,
                              entity = HttpEntity.Strict(
                                ContentTypes.`application/json`,
                                ByteString(authBodyTemplate.format(conn.user, conn.pass, conn.org))
                              ))

    HttpResult.right(Http().singleRequest(request, settings = cp))
  }

  private def extractJwtToken(response: HttpResponse): Xor[Failures, String] = {
    val token = response.headers.find(_.name() == jwtHeaderName).map(_.value())
    Xor.fromOption(token, GeneralFailure(s"Can't extract token from response: $response").single)
  }

  private def getRequest(suffix: String, token: String): HttpResult = {
    val request =
      HttpRequest(HttpMethods.GET, fullUri(suffix)).addHeader(RawHeader(authHeaderName, token))
    HttpResult.right(Http().singleRequest(request, settings = cp))
  }

  private def postRequest(suffix: String, body: String, token: String): HttpResult = {
    val request = HttpRequest(method = HttpMethods.POST,
                              uri = fullUri(suffix),
                              entity = HttpEntity.Strict(
                                ContentTypes.`application/json`,
                                ByteString(body)
                              )).addHeader(RawHeader(authHeaderName, token))

    HttpResult.right(Http().singleRequest(request, settings = cp))
  }

  private def fullUri(suffix: String) = s"${conn.uri}/$suffix"
}

/**
  * Stolen from phoenix
  */
object HttpResponseExtensions {

  implicit class RichHttpResponse(val res: HttpResponse) extends AnyVal {
    def bodyText(implicit ec: EC, mat: AM): Future[String] =
      res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { b ⇒
        b.utf8String
      }
  }
}
