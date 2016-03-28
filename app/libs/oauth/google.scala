package libs.oauth

import scala.concurrent.Future

import cats.data.XorT
import dispatch.{Http, as, url ⇒ request}
import org.json4s._
import utils.aliases._

final case class GoogleOauthOptions(
  clientId: String,
  clientSecret: String,
  redirectUri: String,
  accessType: String = "offline",
  hostedDomain: Option[String] = None
) extends OauthClientOptions {

  override def buildExtraAuthParams: Map[String, String] = {
    Map.empty[String, String]
        .+? ("hd", hostedDomain)
        .+  ("access_type" → accessType)
  }
}

trait GoogleProvider extends OauthProvider {

  val oauthAuthorizationUrl = "https://accounts.google.com/o/oauth2/auth"
  val oauthAccessTokenUrl = "https://accounts.google.com/o/oauth2/token"
  val oauthInfoUrl = "https://www.googleapis.com/oauth2/v1/userinfo"

  def userInfo(accessToken: String)(implicit ec: EC): XorT[Future, Throwable, UserInfo] = xorTryFuture {
    val req = request(oauthInfoUrl).GET.addHeader("Authorization", s"Bearer ${accessToken}")
    Http(req OK as.json4s.Json).map(_.extract[UserInfo])
  }
}
