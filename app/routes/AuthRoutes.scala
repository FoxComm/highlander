package routes

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.auth.Identity
import services.Authenticator
import services.auth.GoogleOauth
import services.auth.OauthService._
import utils.Http._
import utils.aliases._


object AuthRoutes {

  val customerGoogleOauth = GoogleOauth(Identity.Customer.toString.toLowerCase)
  val adminGoogleOauth = GoogleOauth(Identity.Admin.toString.toLowerCase)

  def routes(implicit ec: EC, db: DB, mat: Materializer) = {

    pathPrefix("public") {
      (post & path("login") & entity(as[payloads.LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          result.fold({ f ⇒ complete(renderFailure(f)) }, identity)
        }
      } ~
      (path("oauth2callback" / "google" / "admin") & get & oauthResponse) {
        adminGoogleOauth.callback
      } ~
      (path("oauth2callback" / "google" / "customer") & get & oauthResponse) {
        customerGoogleOauth.callback
      } ~
      (path("signin" / "google" / "admin") & get) {
        val url = adminGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
        complete(Map("url" → url))
      } ~
      (path("signin" / "google" / "customer") & get) {
        val url = customerGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
        complete(Map("url" → url))
      }
    }
  }
}
