package libs.oauth

import scala.concurrent.Future
import dispatch.{Http, as, url ⇒ request}
import utils.aliases._


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

  /* "Returns the OAuth authorization url." */
  def authorizationUri(scopes: Array[String], params: Map[String, String] = Map.empty): String = {
    request(oauthAuthorizationUrl)
      .<<?(Map(
        "client_id" → clientId,
        "redirect_uri" → redirectUri,
        "response_type" → "code",
        "scope" → scopes.mkString(" ")
      ))
      .<<?(params)
      .url
  }

  def accessToken(code: String, grantType: Option[String] = None)(implicit ec: EC): Future[String] = {
    val req = request(oauthAccessTokenUrl)
      .POST
      .<<?(Map(
          "client_id" → clientId,
          "client_secret" → clientSecret,
          "code" → code,
          "redirect_uri" → redirectUri,
          "grant_type" → grantType.getOrElse("authorization_code")))
    Http(req OK as.String)
  }

}