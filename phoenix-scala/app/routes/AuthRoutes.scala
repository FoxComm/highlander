package routes

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import payloads.LoginPayload
import payloads.UserPayloads._
import services.Authenticator
import services.account.AccountManager
import services.auth.GoogleOauth.oauthServiceFromConfig
import services.auth.GoogleOauthUser
import services.auth.OauthDirectives._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing._

object AuthRoutes {

  def routes(implicit ec: EC, db: DB, tr: TracingRequest, trace: TracingExtensionImpl) = {

    pathPrefix("public") {
      (post & path("login") & entity(as[LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          traceEnd {
            result.fold({ f ⇒
              complete(renderFailure(f))
            }, identity)
          }
        }
      } ~
      activityContext() { implicit ac ⇒
        (post & path("send-password-reset") & pathEnd & entity(as[ResetPasswordSend])) { payload ⇒
          mutateOrFailures {
            traceEnd {
              AccountManager.resetPasswordSend(payload.email)
            }
          }
        } ~
        (post & path("reset-password") & pathEnd & entity(as[ResetPassword])) { payload ⇒
          mutateOrFailures {
            traceEnd {
              AccountManager.resetPassword(code = payload.code, newPassword = payload.newPassword)
            }
          }
        }
      } ~
      (post & path("logout")) {
        deleteCookie("JWT", path = "/") {
          traceEnd {
            redirect(Uri("/"), StatusCodes.Found)
          }
        }
      } ~
      activityContext() { implicit ac ⇒
        lazy val customerGoogleOauth = oauthServiceFromConfig("customer")
        lazy val adminGoogleOauth    = oauthServiceFromConfig("admin")

        (path("oauth2callback" / "google" / "admin") & get & oauthResponse) {
          traceEnd {
            adminGoogleOauth.adminCallback
          }
        } ~
        (path("oauth2callback" / "google" / "customer") & get & oauthResponse) {
          traceEnd {
            customerGoogleOauth.customerCallback
          }
        } ~
        (path("signin" / "google" / "admin") & get) {
          val url = adminGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
          complete(
              traceEnd {
                Map("url" → url)
              }
          )
        } ~
        (path("signin" / "google" / "customer") & get) {
          val url = customerGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
          complete(
              traceEnd {
                Map("url" → url)
              }
          )
        }
      }
    }
  }
}
