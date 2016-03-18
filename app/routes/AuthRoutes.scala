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
import utils.Config.{config, RichConfig}
import models.auth.Identity
import cats.data.{XorT, Xor}
import cats.implicits._

import OauthResponse._

import libs.oauth.{Oauth, GoogleProvider, GoogleOauthOptions}

// TODO: move it outside
object OauthResponse {

  final case class OauthCallbackResponse(
    code: Option[String] = None,
    error: Option[String] = None)

  def parseOauthResponse(resp: OauthCallbackResponse): Xor[Throwable, String] = {
    if (resp.error.isEmpty && resp.code.nonEmpty) {
      Xor.right(resp.code.getOrElse(""))
    } else {
      Xor.left(new Throwable(resp.error.getOrElse("Unexpected error")))
    }
  }
}


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
        (get & path("google/admin") & oauthResponse) { oauthResponse ⇒
            /*
            1. Exchange code to access token - Done
            2. Get user email - Done
            3. FindOrCreate<StoreAdmin|Customer> <- ??
            4. respondWithToken
             */
            // FIXME: handle errors

            val result = for {
              code ← XorT.fromXor[Future](parseOauthResponse(oauthResponse))
              accessTokenResp ← adminGoogleOauth.accessToken(code)
              email ← adminGoogleOauth.userEmail(accessTokenResp.access_token)
            } yield email

            onSuccess(result.fold(
              { err ⇒ complete(renderFailure(GeneralFailure(err.toString).single)) },
              { email ⇒ complete(email) }
            ))(identity)

          }
      } ~
      pathPrefix("signin") {
        (get & path("google/admin")) {
          val url = adminGoogleOauth.authorizationUri(scope = Seq("openid", "email", "profile"))
          complete(Map("url" → url))
        }
      }
    }

  }
}
