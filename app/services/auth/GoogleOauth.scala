package services.auth

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.auth.AdminToken
import models.{StoreAdmin, StoreAdmins}
import services.Authenticator
import utils.Config._
import utils.Http._
import services.auth.OauthService._
import utils.DbResultT.implicits._
import utils.aliases._

class GoogleOauth(options: GoogleOauthOptions) extends Oauth(options) with GoogleProvider { self ⇒

  def callback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB): Route = {
      onSuccess(oauthCallback(self, oauthResponse,
        StoreAdmins.findByEmail _,
        (userInfo: UserInfo) ⇒ StoreAdmins.create(StoreAdmin(email = userInfo.email, name = userInfo.name)),
        AdminToken.fromAdmin
      )) { t ⇒
        onSuccess(t.run()) { x ⇒
          x.flatMap(Authenticator.oauthTokenLoginResponse).fold(
            { f ⇒ complete(renderFailure(f)) },
            identity _)
        }
      }
    }

}

object GoogleOauth {

  def apply(configPrefix: String): GoogleOauth = {
    val opts = GoogleOauthOptions(
      clientId = config.getString(s"oauth.$configPrefix.google.client_id"),
      clientSecret = config.getString(s"oauth.$configPrefix.google.client_secret"),
      redirectUri = config.getString(s"oauth.$configPrefix.google.redirect_uri"),
      hostedDomain = config.getOptString(s"oauth.google.$configPrefix.hosted_domain"))


    new GoogleOauth(opts)
  }

}

