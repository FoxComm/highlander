package libs.oauth

import scala.concurrent.Future

import cats.data.XorT
import dispatch.{Http, as, url ⇒ request}
import org.json4s._
import utils.aliases._

case class UserInfo(name: String, email: String)

trait OauthProvider {
  val oauthAuthorizationUrl: String
  val oauthAccessTokenUrl: String

  def userInfo(accessToken: String)(implicit ec: EC): XorT[Future, Throwable, UserInfo]
}

trait OauthClientOptions {
  val clientId: String
  val clientSecret: String
  val redirectUri: String

  def buildExtraAuthParams: Map[String, String] = Map.empty
}

abstract class Oauth(oauthOptions: OauthClientOptions) extends OauthProvider {

  val authorizationParams = Map(
    "client_id"     → oauthOptions.clientId,
    "redirect_uri"  → oauthOptions.redirectUri,
    "response_type" → "code"
  )

  /* "Returns the OAuth authorization url." */
  def authorizationUri(scope: Seq[String]): String = {
    // TODO: add state
    request(oauthAuthorizationUrl)
      .<<?(authorizationParams + ("scope" → scope.mkString(" ")))
      .<<?(oauthOptions.buildExtraAuthParams)
      .url
  }

  def accessToken(code: String)(implicit ec: EC): XorT[Future, Throwable, AccessTokenResponse] =
    xorTryFuture {
      val req = request(oauthAccessTokenUrl).POST.<<(
        Map("client_id"     → oauthOptions.clientId,
            "client_secret" → oauthOptions.clientSecret,
            "code"          → code,
            "redirect_uri"  → oauthOptions.redirectUri,
            "grant_type"    → "authorization_code"))

      Http(req OK as.json4s.Json).map(_.extract[AccessTokenResponse])
    }
}
