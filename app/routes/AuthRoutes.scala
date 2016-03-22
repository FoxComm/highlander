package routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import akka.stream.Materializer

import services.Authenticator
import services.GeneralFailure
import scala.concurrent.Future

import utils.Http._
import utils.aliases._
import utils.Config.{RichConfig, config}
import models.auth.{AdminToken, Identity}
import models.{StoreAdmin, StoreAdmins}
import utils.Slick.implicits._
import cats.data.{Xor, XorT}
import cats.implicits._
import services.OauthService._
import utils.DbResultT._
import utils.DbResultT.implicits._
import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth}



object AuthRoutes {

  def oauthResponse: Directive1[OauthCallbackResponse] =
    parameterMap.map { params ⇒
      OauthCallbackResponse(code = params.get("code"), error = params.get("error"))
    }

  def getGoogleOauthOptions(kind: Identity.IdentityKind): GoogleOauthOptions = {
    val kindStr = kind.toString.toLowerCase

    GoogleOauthOptions(
      clientId = config.getString(s"oauth.$kindStr.google.client_id"),
      clientSecret = config.getString(s"oauth.$kindStr.google.client_secret"),
      redirectUri = config.getString(s"oauth.$kindStr.google.redirect_uri"),
      hostedDomain = config.getOptString(s"oauth.google.$kindStr.hosted_domain"))
  }

  val customerOauthOptions = getGoogleOauthOptions(Identity.Customer)
  val adminOauthOptions = getGoogleOauthOptions(Identity.Admin)

  val customerGoogleOauth = new Oauth(customerOauthOptions) with GoogleProvider
  val adminGoogleOauth = new Oauth(adminOauthOptions) with GoogleProvider

  def routes(implicit ec: EC, db: DB, mat: Materializer) = {

    pathPrefix("public") {
      (post & path("login") & entity(as[payloads.LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          result.fold({ f ⇒ complete(renderFailure(f)) }, identity)
        }
      } ~
      pathPrefix("oauth2callback") {
        (get & pathPrefix("google") & path("admin") & oauthResponse) { oauthResponse ⇒

          onSuccess(oauthCallback(adminGoogleOauth, oauthResponse,
            StoreAdmins.findByEmail _,
            (email: String) ⇒ StoreAdmins.create(StoreAdmin(email = email, name = "Xx Xxx")),
            AdminToken.fromAdmin
          )) { t ⇒
            onSuccess(t.run()) { x ⇒
              x.flatMap(Authenticator.respondWithToken).fold( { f ⇒ complete(renderFailure(f)) }, identity)
            }
          }

          }
      } ~
      pathPrefix("signin") {
        (pathPrefix("google") & get & path("admin")) {
          val url = adminGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
          complete(Map("url" → url))
        }
      }
    }

  }
}
