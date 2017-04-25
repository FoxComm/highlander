package libs.oauth

import cats.data.EitherT
import dispatch.{Http, url ⇒ request}
import scala.concurrent.Future
import utils.aliases._
import utils.json._

case class GoogleOauthOptions(
    roleName: String,
    scopeId: Int, //must retrieve from db for now.
    orgName: String,
    clientId: String,
    clientSecret: String,
    redirectUri: String,
    accessType: String = "offline",
    hostedDomain: Option[String] = None
) extends OauthClientOptions {

  override def buildExtraAuthParams: Map[String, String] = {
    Map.empty[String, String].+?("hd", hostedDomain).+("access_type" → accessType)
  }
}

trait GoogleProvider extends OauthProvider {

  val oauthAuthorizationUrl = "https://accounts.google.com/o/oauth2/auth"
  val oauthAccessTokenUrl   = "https://accounts.google.com/o/oauth2/token"
  val oauthInfoUrl          = "https://www.googleapis.com/oauth2/v1/userinfo"

  def userInfo(accessToken: String)(implicit ec: EC): EitherT[Future, Throwable, UserInfo] =
    eitherTryFuture {
      val req = request(oauthInfoUrl).GET.addHeader("Authorization", s"Bearer $accessToken")
      Http(req OK asJson).map(_.as[UserInfo])
    }
}
