package consumer.utils

import scala.concurrent.Future

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString

import consumer.aliases._

import scala.language.postfixOps

final case class PhoenixConnectionInfo(uri: String, user: String, pass: String)

case class Phoenix(conn: PhoenixConnectionInfo)(implicit ec: EC, ac: AS, mat: AM, cp: CP) {

  val authUri = fullUri("public/login")

  val authBodyTemplate = """{"email": "%s", "password": "%s"}"""

  val jwtHeaderName = "JWT"

  val authHeaderName = "Authorization"

  def auth(): Future[HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = authUri,
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(authBodyTemplate.format(conn.user, conn.pass))
      ))

    Http().singleRequest(request, cp)
  }

  def get(suffix: String): Future[HttpResponse] = for {
    authResponse ← auth()
    jwtToken     = extractJwtToken(authResponse)
    response     ← getInner(suffix, jwtToken)
  } yield response

  def post(suffix: String, body: String): Future[HttpResponse] = for {
    authResponse ← auth()
    jwtToken     = extractJwtToken(authResponse)
    response     ← postInner(suffix, body, jwtToken)
  } yield response

  private def getInner(suffix: String, token: String): Future[HttpResponse] = {
    val request = HttpRequest(HttpMethods.GET, fullUri(suffix)).addHeader(RawHeader(authHeaderName, token))
    Console.out.println(s"JWT Token: $token")
    Http().singleRequest(request, cp)
  }

  private def postInner(suffix: String, body: String, token: String): Future[HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = fullUri(suffix),
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(body)
      )).addHeader(RawHeader(authHeaderName, token))

    Console.out.println(s"JWT Token: $token")
    Http().singleRequest(request, cp)
  }

  private def fullUri(suffix: String) = s"${conn.uri}/$suffix"

  private def extractJwtToken(response: HttpResponse): String =
    response.headers.find(_.name() == jwtHeaderName).map(_.value()).getOrElse("")
}

/**
 * Stolen from phoenix
 */
object HttpResponseExtensions {

  implicit class RichHttpResponse(val res: HttpResponse) extends AnyVal {
    def bodyText(implicit ec: EC, mat: AM): Future[String] = res.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
      .map { b ⇒ b.utf8String}
  }
}
