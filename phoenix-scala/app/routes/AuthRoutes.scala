package routes

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import payloads.LoginPayload
import payloads.UserPayloads._
import services.Authenticator
import services.account.AccountManager
import services.auth.GoogleOauth.oauthServiceFromConfig
import services.auth.OauthDirectives._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object AuthRoutes {

  def routes(implicit ec: EC, db: DB) = {

    (post & path("login") & entity(as[LoginPayload])) { payload ⇒
      onSuccess(Authenticator.authenticate(payload)) { result ⇒
        result.fold({ f ⇒
          complete(renderFailure(f))
        }, identity)
      }
    } ~
    activityContext() { implicit ac ⇒
      (post & path("send-password-reset") & pathEnd & entity(as[ResetPasswordSend])) { payload ⇒
        mutateOrFailures {
          AccountManager.resetPasswordSend(payload.email)
        }
      } ~
      (post & path("reset-password") & pathEnd & entity(as[ResetPassword])) { payload ⇒
        mutateOrFailures {
          AccountManager.resetPassword(code = payload.code, newPassword = payload.newPassword)
        }
      }
    } ~
    (post & path("logout")) {
      deleteCookie("JWT", path = "/") {
        redirect(Uri("/"), StatusCodes.Found)
      }
    } ~
    activityContext() { implicit ac ⇒
      lazy val customerGoogleOauth = oauthServiceFromConfig("customer")
      lazy val adminGoogleOauth    = oauthServiceFromConfig("admin")

      (get & path("oauth2callback" / "google" / "admin") & pathEnd & oauthResponse) {
        adminGoogleOauth.adminCallback
      } ~
      (get & path("oauth2callback" / "google" / "customer") & pathEnd & oauthResponse) {
        customerGoogleOauth.customerCallback
      } ~
      (get & path("signin" / "google" / "admin") & pathEnd) {
        val url = adminGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
        complete(Map("url" → url))
      } ~
      (get & path("signin" / "google" / "customer") & pathEnd) {
        val url = customerGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
        complete(Map("url" → url))
      }
    }
  }
}
