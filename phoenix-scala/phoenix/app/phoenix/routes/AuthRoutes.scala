package phoenix.routes

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.util.Tuple
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import phoenix.payloads.LoginPayload
import phoenix.payloads.UserPayloads._
import phoenix.responses.UserResponse.ResetPasswordDoneAnswer
import phoenix.services.Authenticator
import phoenix.services.account.AccountManager
import phoenix.services.auth.GoogleOauth.oauthServiceFromConfig
import phoenix.services.auth.OauthDirectives._
import phoenix.utils.FoxConfig.config
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object AuthRoutes {

  def routes(defaultScope: LTree)(implicit ec: EC, db: DB): Route = {

    def doLogin(payload: LoginPayload): Route =
      onSuccess(Authenticator.authenticate(payload).runEmptyA.value) { result ⇒ // TODO: rethink discarding warnings here @michalrus
        result.fold({ f ⇒
          complete(renderFailure(f))
        }, identity)
      }

    pathPrefix("public") {
      (post & path("login") & entity(as[LoginPayload])) { payload ⇒
        doLogin(payload)
      } ~
      activityContext(defaultScope) { implicit ac ⇒
        (post & path("send-password-reset") & pathEnd & entity(as[ResetPasswordSend])) { payload ⇒
          mutateOrFailures {
            AccountManager.resetPasswordSend(payload.email)
          }
        } ~
        (post & path("reset-password") & pathEnd & entity(as[ResetPassword])) { payload ⇒
          implicit val payloadTuple: Tuple[ResetPasswordDoneAnswer] = Tuple.yes

          mutateOrFailures {
            AccountManager.resetPassword(code = payload.code, newPassword = payload.newPassword)
          }.toDirective[ResetPasswordDoneAnswer]
            .tapply(answer ⇒
                  doLogin(LoginPayload(email = answer.email,
                                       password = payload.newPassword,
                                       org = answer.org)))
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
