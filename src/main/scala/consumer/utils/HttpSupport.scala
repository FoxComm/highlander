package consumer.utils

import scala.concurrent.Future

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString

import consumer.aliases._

import scala.language.postfixOps

final case class PhoenixConnectionInfo(uri: String, user: String, pass: String)

case class Phoenix(conn: PhoenixConnectionInfo)(implicit ec: EC, ac: AS, mat: AM, cp: CP) {

  def get(suffix: String) : Future[HttpResponse] = {
    val uri  = fullUri(suffix)
    val request = HttpRequest(HttpMethods.GET,uri)
      .addHeader(Authorization(BasicHttpCredentials(conn.user, conn.pass)))

    Http().singleRequest(request, cp)
  }

  def post(suffix: String, body: String) : Future[HttpResponse]= {
    val uri  = fullUri(suffix)
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri    = uri,
      entity = HttpEntity.Strict(
        ContentTypes.`application/json`,
        ByteString(body)
      )).addHeader(Authorization(BasicHttpCredentials(conn.user, conn.pass)))

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
