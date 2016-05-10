package consumer.utils

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString

import consumer.aliases._

import scalacache._
import lrumap._
import memoization._

import scala.language.postfixOps

final case class PhoenixConnectionInfo(uri: String, user: String, pass: String)

case class Phoenix(conn: PhoenixConnectionInfo)(implicit ec: EC, ac: AS, mat: AM, cp: CP) {

  implicit val scalaCache = ScalaCache(LruMapCache(1))

  val authUri = fullUri("public/login")

  val authBodyTemplate = """{"email": "%s", "password": "%s", "kind": "admin"}"""

  val jwtHeaderName = "JWT"

  val authHeaderName = "JWT"

  def get(suffix: String): Future[HttpResponse] = for {
    jwtToken ← getJwtToken
    response ← getRequest(suffix, jwtToken)
  } yield response

  def post(suffix: String, body: String): Future[HttpResponse] = for {
    jwtToken ← getJwtToken
    response ← postRequest(suffix, body, jwtToken)
  } yield response

  private def getJwtToken: Future[String] = memoize(7 days) {
    authenticate().map(extractJwtToken)
  }

  private def authenticate(): Future[HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = authUri,
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(authBodyTemplate.format(conn.user, conn.pass))
      ))

    Http().singleRequest(request, cp)
  }

  private def extractJwtToken(response: HttpResponse): String =
    response.headers.find(_.name() == jwtHeaderName).map(_.value()).getOrElse("")

  private def getRequest(suffix: String, token: String): Future[HttpResponse] = {
    val request = HttpRequest(HttpMethods.GET, fullUri(suffix)).addHeader(RawHeader(authHeaderName, token))
    Http().singleRequest(request, cp)
  }

  private def postRequest(suffix: String, body: String, token: String): Future[HttpResponse] = {
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = fullUri(suffix),
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(body)
      )).addHeader(RawHeader(authHeaderName, token))
    
    Http().singleRequest(request, cp)
  }

  private def fullUri(suffix: String) = s"${conn.uri}/$suffix"
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
