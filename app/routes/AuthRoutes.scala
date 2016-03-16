package routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import akka.stream.Materializer
import services.Authenticator
import services.GeneralFailure
import utils.Http._
import utils.aliases._
import utils.Config.config

import OauthResponse._
import cats.data.Xor

import libs.oauth.{Oauth, GoogleProvider, OauthClientCredentials}

// TODO: move it outside
object OauthResponse {

  final case class OauthCallbackResponse(
    code: Option[String] = None,
    error: Option[String] = None)

  def parseOauthResponse(resp: OauthCallbackResponse): Xor[String, String] = {
    if (resp.error.isEmpty && resp.code.nonEmpty) {
      Xor.right(resp.code.getOrElse(""))
    } else {
      Xor.left(resp.error.getOrElse("Unexpected error"))
    }
  }
}


object AuthRoutes {

  def oauthResponse: Directive1[OauthCallbackResponse] =
    parameterMap.map { params ⇒
      OauthCallbackResponse(code = params.get("code"), error = params.get("error"))
    }

  trait GoogleOauthCredentials extends OauthClientCredentials {
    val clientId = config.getString("oauth.google.client_id")
    val clientSecret = config.getString("oauth.google.client_secret")
    val redirectUri = config.getString("oauth.google.redirect_uri")
  }

  case object GoogleOauth extends Oauth with GoogleProvider with GoogleOauthCredentials

  def routes(implicit ec: EC, db: DB, mat: Materializer) = {

    pathPrefix("public") {
      (post & path("login") & entity(as[payloads.LoginPayload])) { payload ⇒
        onSuccess(Authenticator.authenticate(payload)) { result ⇒
          result.fold({ f ⇒ complete(renderFailure(f)) }, identity)
        }
      } ~
      pathPrefix("oauth2callback") {
        (get & path("google") & oauthResponse) { oauthResponse ⇒
          parseOauthResponse(oauthResponse).fold(
            { err ⇒ complete(renderFailure(GeneralFailure(err).single)) },
            { code ⇒
              /*
              1. Exchange code to access token - Done
              2. Get user email - Done
              3. FindOrCreate<StoreAdmin|Customer> <- ??
              4. respondWithToken
               */
              // FIXME: handle errors
              onSuccess(for {
                accessTokenResp ← GoogleOauth.accessToken(code)
                email ← GoogleOauth.userEmail(accessTokenResp.access_token)
              } yield email) { email ⇒
                complete(email)
              }


            }
          )
        }
      } ~
      pathPrefix("signin") {
        (get & path("google")) {
          val uri = GoogleOauth.authorizationUri(scopes = Array("email"))
          complete(Map("url" → uri))
        }
      }
    }

  }
}
