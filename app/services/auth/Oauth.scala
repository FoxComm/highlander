package services.auth

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import cats.data.{Xor, XorT}
import cats.implicits._
import failures.GeneralFailure
import libs.oauth.{Oauth, UserInfo}
import models.auth.Token
import services.Authenticator
import slick.driver.PostgresDriver.api.DBIO
import utils.http.Http._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

case class OauthCallbackResponse(code: Option[String] = None, error: Option[String] = None) {

  def getCode: Xor[Throwable, String] =
    if (this.error.isEmpty && this.code.nonEmpty) {
      Xor.right(this.code.getOrElse(""))
    } else {
      Xor.left(new Throwable(this.error.getOrElse("Unexpected error")))
    }
}

object OauthDirectives {

  def oauthResponse: Directive1[OauthCallbackResponse] =
    parameterMap.map { params ⇒
      OauthCallbackResponse(code = params.get("code"), error = params.get("error"))
    }
}

trait OauthService[M] {
  this: Oauth ⇒

  def createByUserInfo(info: UserInfo)(implicit ec: EC): DbResult[M]
  def findByEmail(email: String)(implicit ec: EC, db: DB): DBIO[Option[M]]
  def createToken(user: M): Token

  /*
    1. Exchange code to access token
    2. Get base user info: email and name
   */
  def fetchUserInfoFromCode(oauthResponse: OauthCallbackResponse)(
      implicit ec: EC): DbResultT[UserInfo] = {
    for {
      code ← XorT
              .fromXor[DBIO](oauthResponse.getCode)
              .leftMap(t ⇒ GeneralFailure(t.toString).single)
      accessTokenResp ← * <~ this
                         .accessToken(code)
                         .leftMap(t ⇒ GeneralFailure(t.toString).single)
                         .value
      info ← * <~ this
              .userInfo(accessTokenResp.access_token)
              .leftMap(t ⇒ GeneralFailure(t.toString).single)
              .value
    } yield info
  }

  def findOrCreateUserFromInfo(userInfo: UserInfo)(implicit ec: EC, db: DB): DbResultT[M] =
    for {
      result ← * <~ findByEmail(userInfo.email).findOrCreateExtended(createByUserInfo(userInfo))
      (user, foundOrCreated) = result
    } yield user

  /*
    1. Exchange code to access token
    2. Get base user info: email and name
    3. FindOrCreate<UserModel>
    4. respondWithToken
   */
  def oauthCallback(
      oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB): DbResultT[Token] =
    for {
      info ← fetchUserInfoFromCode(oauthResponse)
      user ← findOrCreateUserFromInfo(info)
      token = createToken(user)
    } yield token

  def akkaCallback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB): Route = {
    onSuccess(oauthCallback(oauthResponse).run()) { tokenOrFailure ⇒
      tokenOrFailure
        .flatMap(Authenticator.oauthTokenLoginResponse)
        .fold({ f ⇒
          complete(renderFailure(f))
        }, identity _)
    }
  }
}
