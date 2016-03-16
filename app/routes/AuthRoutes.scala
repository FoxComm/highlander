package routes

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import akka.stream.Materializer
import services.Authenticator
import utils.Http._
import utils.aliases._
import utils.Config.config

import libs.oauth.{Oauth, GoogleProvider, OauthClientCredentials}

object AuthRoutes {

  trait GoogleOauthCredentials extends OauthClientCredentials {
    val clientId = config.getString("oauth.google.client_id")
    val clientSecret = config.getString("oauth.google.client_secret")
    val redirectUri = config.getString("oauth.google.redirect_uri")
  }

  case object GoogleOauth extends Oauth with GoogleProvider with GoogleOauthCredentials

  def routes(implicit ec: EC, db: DB, mat: Materializer) = {

    pathPrefix("public") {
      (post & path("login") & entity(as[payloads.LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          result.fold({ f ⇒ complete(renderFailure(f)) }, identity)
        }
      } ~
      pathPrefix("oauth2callback") {
        (get & path("google")) {
          complete("TODO")
        }
      } ~
      pathPrefix("signin") {
        (get & path("google")) {
          val uri = GoogleOauth.authorizationUri(scopes = Array("email"))
          complete(Map("url" → uri))
        }
      }
    }

  }
}
