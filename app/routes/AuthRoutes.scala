package routes

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.auth.Identity
import services.Authenticator
import services.auth.GoogleOauth.oauthServiceFromConfig
import services.auth.OauthDirectives._
import utils.Http._
import utils.aliases._


object AuthRoutes {

  lazy val customerGoogleOauth = oauthServiceFromConfig(Identity.Customer)
  lazy val adminGoogleOauth = oauthServiceFromConfig(Identity.Admin)

  def routes(implicit ec: EC, db: DB, mat: Materializer) = {
    pathPrefix("public") {
      (post & path("login") & entity(as[payloads.LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          result.fold({ f ⇒ complete(renderFailure(f)) }, identity)
        }
      } ~
      (post & path("logout")) {
        deleteCookie("JWT", path = "/") {
          redirect(Uri("/"), StatusCodes.Found)
        }
      } ~
      (path("oauth2callback" / "google" / "admin") & get & oauthResponse) {
        adminGoogleOauth.akkaCallback
      } ~
      (path("oauth2callback" / "google" / "customer") & get & oauthResponse) {
        customerGoogleOauth.akkaCallback
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
