package phoenix.routes

import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import phoenix.payloads.LoginPayload
import phoenix.payloads.UserPayloads._
import phoenix.services.Authenticator
import phoenix.services.account.AccountManager
import phoenix.services.auth.OauthServices
import phoenix.services.auth.OauthDirectives._
import phoenix.utils.FoxConfig.config
import phoenix.utils.FoxConfig.SupportedOauthProviders.OauthProvider
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object AuthRoutes {

  def routes(defaultScope: LTree)(implicit ec: EC, db: DB, apis: Apis): Route =
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
        }
    } ~
      (post & path("logout")) {
        deleteCookie("JWT", path = "/") {
          redirect(Uri("/"), StatusCodes.Found)
        }
      } ~
      activityContext(defaultScope) { implicit ac ⇒
        (path("oauthtest" / OauthProvider)) { prov ⇒
          complete(prov.show)
        } ~
          (path("oauth2callback" / OauthProvider / "admin") & get & oauthResponse) { (provider, resp) ⇒
            OauthServices.get(provider).admin.adminCallback(resp)
          } ~
          (path("oauth2callback" / OauthProvider / "customer") & get & oauthResponse) { (provider, resp) ⇒
            OauthServices.get(provider).customer.customerCallback(resp)
          } ~
          (path("signin" / OauthProvider / "admin") & get) { provider ⇒
            val url = OauthServices
              .get(provider)
              .admin
              .authorizationUri(scope = Seq("openid", "email", "profile"))
            complete(Map("url" → url))
          } ~
          (path("signin" / OauthProvider / "customer") & get) { provider ⇒
            val url = OauthServices
              .get(provider)
              .customer
              .authorizationUri(scope = Seq("openid", "email", "profile"))
            complete(Map("url" → url))
          }
      }
}
