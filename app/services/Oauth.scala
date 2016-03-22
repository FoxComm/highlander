package services

import scala.concurrent.Future

import cats.data.{Xor, XorT}
import cats.implicits._
import libs.oauth.Oauth
import models.auth.Token
import slick.dbio.DBIO
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases.EC
import utils.DbResultT

object OauthService {

  type EmailFinder[M] = String ⇒ DBIO[Option[M]]
  type EmailCreate[M] = String ⇒ DbResult[M]

  final case class OauthCallbackResponse(
    code: Option[String] = None,
    error: Option[String] = None)


  def parseOauthResponse(resp: OauthCallbackResponse): Xor[Throwable, String] = {
    if (resp.error.isEmpty && resp.code.nonEmpty) {
      Xor.right(resp.code.getOrElse(""))
    } else {
      Xor.left(new Throwable(resp.error.getOrElse("Unexpected error")))
//      Xor.left(GeneralFailure(resp.error.getOrElse("Unexpected error")).single)
    }
  }


  /*
    1. Exchange code to access token - Done
    2. Get user email - Done
    3. FindOrCreate<StoreAdmin|Customer> <- ??
    4. respondWithToken
  */
  def oauthCallback[O <: Oauth, M, F <: EmailFinder[M], C <: EmailCreate[M]]
  (oauth: O, oauthResponse: OauthCallbackResponse, findByEmail: F, createByEmail: C, createToken: M ⇒ Token)
    (implicit ec: EC) = {

    val tokenFromEmail = (email: String) ⇒ for {
      result ← * <~ findByEmail(email).findOrCreateExtended(createByEmail(email))
      (user, foundOrCreated) = result
      token = createToken(user)
    } yield token

    val emailX = for {
      code ← XorT.fromXor[Future](parseOauthResponse(oauthResponse))
      accessTokenResp ← oauth.accessToken(code)
      email ← oauth.userEmail(accessTokenResp.access_token)
    } yield email

    emailX.fold({ t ⇒ DbResultT.leftLift(GeneralFailure(t.toString).single) }, tokenFromEmail)

  }

}
