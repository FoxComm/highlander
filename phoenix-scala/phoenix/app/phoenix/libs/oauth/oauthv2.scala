package phoenix.libs.oauth

import cats.data.EitherT
import cats.implicits._
import dispatch.{Http, as, url ⇒ request}
import akka.http.scaladsl.model.Uri

import org.json4s._
import phoenix.utils.aliases._
import scala.concurrent.Future

import core.failures.Failures
import phoenix.failures.OauthFailures._

case class UserInfo(name: String, email: String) {
  def emailDomain: Either[Failures, String] = {
    val userAtDomain = email.split("@")
    if (userAtDomain.size == 2)
      Either.right(userAtDomain(1))
    else
      Either.left(InvalidEmailInUserInfo.single)
  }
}

trait OauthProvider {
  val oauthAuthorizationUrl: String
  val oauthAccessTokenUrl: String

  def mkScopes(scopes: Set[String]): String

  def userInfo(accessToken: String)(implicit ec: EC): EitherT[Future, Throwable, UserInfo]
}

trait OauthClientOptions {
  val clientId: String
  val clientSecret: String
  val redirectUri: String
  val scopes: Seq[String]

  def buildExtraAuthParams: Map[String, String] = Map.empty
}

/* implements required standard flows of Oauth used by phoenix */
abstract class Oauth(oauthOptions: OauthClientOptions) extends OauthProvider {

  val authorizationParams = Map(
    "client_id"     → oauthOptions.clientId,
    "redirect_uri"  → oauthOptions.redirectUri,
    "response_type" → "code"
  )

  /* "Returns the OAuth authorization url." */
  // TODO: add state support
  lazy val authorizationUri: Uri = {
    val params = authorizationParams ++
      Map[String, String]("scope" → mkScopes(oauthOptions.scopes.toSet)) ++
      oauthOptions.buildExtraAuthParams

    Uri(oauthAuthorizationUrl).withQuery(Uri.Query(params))
  }

  def accessToken(code: String)(implicit ec: EC): EitherT[Future, Throwable, AccessTokenResponse] =
    eitherTryFuture {
      val req = request(oauthAccessTokenUrl).POST.<<(
        Map(
          "client_id"     → oauthOptions.clientId,
          "client_secret" → oauthOptions.clientSecret,
          "code"          → code,
          "redirect_uri"  → oauthOptions.redirectUri,
          "grant_type"    → "authorization_code"
        ))

      Http(req OK as.json4s.Json).map(_.extract[AccessTokenResponse])
    }
}
