package libs.oauth

import scala.concurrent.Future
import dispatch.{Http, as, url ⇒ request}
import utils.aliases._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats


trait OauthProvider {
  val oauthAuthorizationUrl: String
  val oauthAccessTokenUrl: String
  val oauthInfoUrl: String
}

trait OauthClientCredentials {
  val clientId: String
  val clientSecret: String
  val redirectUri: String
}

trait Oauth { this: OauthProvider with OauthClientCredentials ⇒

  implicit val formats = DefaultFormats

  /* "Returns the OAuth authorization url." */
  def authorizationUri(scopes: Seq[String], params: Map[String, String] = Map.empty): String = {
    // TODO: add state
    request(oauthAuthorizationUrl)
      .<<?(Map(
        "client_id" → clientId,
        "redirect_uri" → redirectUri,
        "response_type" → "code",
        "scope" → scopes.mkString(" "),
        "access_type" → "offline"
      ))
      .<<?(params)
      .url
  }

  def accessToken(code: String)(implicit ec: EC): Future[AccessTokenResponse] = {
    val req = request(oauthAccessTokenUrl)
      .POST
      .<<(Map(
          "client_id" → clientId,
          "client_secret" → clientSecret,
          "code" → code,
          "redirect_uri" → redirectUri,
          "grant_type" → "authorization_code"))
    Http(req OK as.json4s.Json).map(_.extract[AccessTokenResponse])
  }

  // FIXME: google specific?
  def userEmail(accessToken: String)(implicit ec: EC): Future[String] = {
    val req = request(oauthInfoUrl).GET.addHeader("Authorization", s"Bearer ${accessToken}")
    Http(req OK as.json4s.Json).map(_.extract[EntitywithEmail].email)
  }

}