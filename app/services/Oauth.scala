package services

import scala.concurrent.Future

import cats.data.{Xor, XorT}
import cats.implicits._
import libs.oauth.{Oauth, UserInfo}
import failures.GeneralFailure
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
  type UserCreator[M] = UserInfo ⇒ DbResult[M]

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

  /*
    1. Exchange code to access token
    2. Get base user info email and name
    3. FindOrCreate<UserModel>
    4. respondWithToken
  */
  def oauthCallback[O <: Oauth, M, F <: EmailFinder[M], C <: UserCreator[M]]
  (oauth: O, oauthResponse: OauthCallbackResponse, findByEmail: F, createByUserInfo: C, createToken: M ⇒ Token)
    (implicit ec: EC): Future[DbResultT[Token]] = {

    val tokenFromUserInfo = (info: UserInfo) ⇒ for {
      result ← * <~ findByEmail(info.email).findOrCreateExtended(createByUserInfo(info))
      (user, foundOrCreated) = result
      token = createToken(user)
    } yield token

    val infoX = for {
      code ← XorT.fromXor[Future](parseOauthResponse(oauthResponse))
      accessTokenResp ← oauth.accessToken(code)
      info ← oauth.userInfo(accessTokenResp.access_token)
    } yield info

    infoX.fold({ t ⇒ DbResultT.leftLift(GeneralFailure(t.toString).single) }, tokenFromUserInfo)
  }

}
