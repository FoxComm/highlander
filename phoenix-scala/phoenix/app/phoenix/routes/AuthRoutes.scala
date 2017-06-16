package phoenix.routes

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.db._
import phoenix.payloads.LoginPayload
import phoenix.payloads.UserPayloads._
import phoenix.services.Authenticator
import phoenix.services.account.AccountManager
import phoenix.services.auth.GoogleOauth.oauthServiceFromConfig
import phoenix.services.auth.OauthDirectives._
import phoenix.utils.FoxConfig.config
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object AuthRoutes {

  def routes(defaultScope: LTree)(implicit ec: EC, db: DB, apis: Apis): Route = {

    // FIXME @kjanosz: investigate with higher akka-http version, or subsequent http server
    // There should be a better way to do this, but making auto login on password reset
    // does not seem to work when using akka-http route & directive combinators and conversions
    def doLogin(payload: DbResultT[LoginPayload])(routeUnwrap: DbResultT[Route] ⇒ Result[Route]): Route =
      onSuccess(routeUnwrap(Authenticator.authenticate(payload)).runEmptyA.value) { result ⇒ // TODO: rethink discarding warnings here @michalrus
        result.fold({ f ⇒
          complete(renderFailure(f))
        }, identity)
      }

    pathPrefix("public") {
      (post & path("login") & entity(as[LoginPayload])) { payload ⇒
        doLogin(DbResultT.pure(payload))(_.runDBIO())
      } ~
      activityContext(defaultScope) { implicit ac ⇒
        (post & path("send-password-reset") & pathEnd & entity(as[ResetPasswordSend])) { payload ⇒
          mutateOrFailures {
            AccountManager.resetPasswordSend(payload.email)
          }
        } ~
        (post & path("reset-password") & pathEnd & entity(as[ResetPassword])) { payload ⇒
          val doPasswordReset = AccountManager
            .resetPassword(code = payload.code, newPassword = payload.newPassword)
            .map(
              answer ⇒
                LoginPayload(
                  email = answer.email,
                  password = payload.newPassword,
                  org = answer.org
              ))

          doLogin(doPasswordReset)(_.runTxn())
        } ~
        (post & path("logout")) {
          deleteCookie("JWT", path = "/") {
            redirect(Uri("/"), StatusCodes.Found)
          }
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
