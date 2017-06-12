package phoenix.libs.oauth

import cats.data.EitherT
import dispatch.{Http, as, url ⇒ request}
import org.json4s._
import org.json4s.jackson.{compactJson, parseJson}
import phoenix.utils.aliases._
import scala.concurrent.Future

import com.pellucid.sealerate
import phoenix.utils.ADT

object FacebookOauthOptions {
  sealed trait AuthType
  implicit object AuthType extends ADT[AuthType] {
    case object Rerequest extends AuthType

    def types = sealerate.values[AuthType]
  }
}

import FacebookOauthOptions._

case class FacebookOauthOptions(clientId: String,
                                clientSecret: String,
                                redirectUri: String,
                                authType: Option[AuthType],
                                scopes: Seq[String])
    extends OauthClientOptions {

  override def buildExtraAuthParams: Map[String, String] =
    Map.empty[String, String].+?("auth_type", authType.map(AuthType.show))
}

trait FacebookProvider extends OauthProvider {

  val facebookApiVersion = "v2.9"

  val oauthAuthorizationUrl = s"https://www.facebook.com/$facebookApiVersion/dialog/oauth"
  val oauthAccessTokenUrl   = s"https://graph.facebook.com/$facebookApiVersion/oauth/access_token"
  val oauthInfoUrl          = "https://graph.facebook.com/me?fields=name,email"

  def userInfo(accessToken: String)(implicit ec: EC): EitherT[Future, Throwable, UserInfo] =
    eitherTryFuture {
      val req = request(oauthInfoUrl).GET.addHeader("Authorization", s"Bearer $accessToken")
      Http(req OK as.json4s.Json).map(_.extract[UserInfo])
    }

  override def mkScopes(scopes: Set[String]): String = scopes.mkString(",")
}
