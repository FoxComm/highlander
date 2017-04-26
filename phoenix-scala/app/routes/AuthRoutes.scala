package routes

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import payloads.LoginPayload
import payloads.UserPayloads._
import services.Authenticator
import services.account.AccountManager
import services.auth.GoogleOauth.oauthServiceFromConfig
import services.auth.OauthDirectives._
import utils.FoxConfig.config
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.http.JsonSupport._
import utils.json.codecs._

object AuthRoutes {

  def routes(defaultScope: LTree)(implicit ec: EC, db: DB): Route = {

    pathPrefix("public") {
      (post & path("login") & entity(as[LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload).runEmptyA.value) { result ⇒ // TODO: rethink discarding warnings here @michalrus
          result.fold({ f ⇒
            complete(renderFailure(f))
          }, identity)
        }
      } ~
      activityContext(defaultScope) { implicit ac ⇒
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
      activityContext(defaultScope) { implicit ac ⇒
        lazy val customerGoogleOauth = oauthServiceFromConfig(config.users.customer)
        lazy val adminGoogleOauth    = oauthServiceFromConfig(config.users.admin)

        (path("oauth2callback" / "google" / "admin") & get & oauthResponse) {
          adminGoogleOauth.adminCallback
        } ~
        (path("oauth2callback" / "google" / "customer") & get & oauthResponse) {
          customerGoogleOauth.customerCallback
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
}
