package services.auth

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.Uri

import cats.data.{Xor, XorT}
import cats.implicits._
import failures.GeneralFailure
import libs.oauth.{Oauth, UserInfo}
import models.account._
import models.auth.Token
import services.Authenticator
import slick.driver.PostgresDriver.api.DBIO
import utils.aliases._
import utils.db._
import utils.http.Http._

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

  def getScopeId: Int
  def createCustomerByUserInfo(info: UserInfo): DbResultT[M]
  def createAdminByUserInfo(info: UserInfo): DbResultT[M]
  def findByEmail(email: String): DBIO[Option[M]]
  def createToken(user: M, account: Account, scopeId: Int): DbResultT[Token]
  def findAccount(user: M): DbResultT[Account]

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

  def findOrCreateUserFromInfo(userInfo: UserInfo, createByUserInfo: (UserInfo) ⇒ DbResultT[M])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[(M, Account)] =
    for {
      result ← * <~ findByEmail(userInfo.email).findOrCreateExtended(createByUserInfo(userInfo))
      (user, foundOrCreated) = result
      account ← * <~ findAccount(user)
    } yield (user, account)

  /*
    1. Exchange code to access token
    2. Get base user info: email and name
    3. FindOrCreate<UserModel>
    4. respondWithToken
   */
  def oauthCallback(oauthResponse: OauthCallbackResponse,
                    createByUserInfo: (UserInfo) ⇒ DbResultT[M])(implicit ec: EC,
                                                                 db: DB,
                                                                 ac: AC): DbResultT[Token] =
    for {
      info        ← * <~ fetchUserInfoFromCode(oauthResponse)
      userAccount ← * <~ findOrCreateUserFromInfo(info, createByUserInfo)
      (user, account) = userAccount
      token ← * <~ createToken(user, account, getScopeId)
    } yield token

  def customerCallback(
      oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route = {
    onSuccess(oauthCallback(oauthResponse, createCustomerByUserInfo).run()) { tokenOrFailure ⇒
      tokenOrFailure
        .flatMap(Authenticator.oauthTokenLoginResponse(Uri./))
        .fold({ f ⇒
          complete(renderFailure(f))
        }, identity)
    }
  }

  def adminCallback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route = {
    onSuccess(oauthCallback(oauthResponse, createAdminByUserInfo).run()) { tokenOrFailure ⇒
      tokenOrFailure
        .flatMap(Authenticator.oauthTokenLoginResponse(Uri("/admin")))
        .fold({ f ⇒
          complete(renderFailure(f))
        }, identity)
    }
  }
}
