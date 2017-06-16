package phoenix.services.auth

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import cats.implicits._
import core.db._
import core.failures.GeneralFailure
import phoenix.libs.oauth.{Oauth, UserInfo}
import phoenix.models.account._
import phoenix.models.auth.Token
import phoenix.services.Authenticator
import phoenix.utils.aliases._
import phoenix.utils.http.Http._
import slick.jdbc.PostgresProfile.api.DBIO

case class OauthCallbackResponse(code: Option[String] = None, error: Option[String] = None) {

  def getCode: Either[Throwable, String] =
    if (this.error.isEmpty && this.code.nonEmpty) {
      Either.right(this.code.getOrElse(""))
    } else {
      Either.left(new Throwable(this.error.getOrElse("Unexpected error")))
    }
}

object OauthDirectives {

  def oauthResponse: Directive1[OauthCallbackResponse] =
    parameterMap.map { params ⇒
      OauthCallbackResponse(code = params.get("code"), error = params.get("error"))
    }
}

trait OauthService[M] { this: Oauth ⇒

  def createCustomerByUserInfo(info: UserInfo): DbResultT[M]
  def createAdminByUserInfo(info: UserInfo): DbResultT[M]
  def findByEmail(email: String): DBIO[Option[M]]
  def createToken(user: M, account: Account, info: UserInfo): DbResultT[Token]
  def findAccount(user: M): DbResultT[Account]

  /*
    1. Exchange code to access token
    2. Get base user info: email and name
   */
  def fetchUserInfoFromCode(oauthResponse: OauthCallbackResponse)(implicit ec: EC): DbResultT[UserInfo] =
    for {
      code ← DbResultT.fromEither(oauthResponse.getCode.leftMap(t ⇒ GeneralFailure(t.toString).single))
      accessTokenResp ← * <~ this
                         .accessToken(code)
                         .leftMap(t ⇒ GeneralFailure(t.toString).single)
                         .value
      info ← * <~ this
              .userInfo(accessTokenResp.access_token)
              .leftMap(t ⇒ GeneralFailure(t.toString).single)
              .value
    } yield info

  def findOrCreateUserFromInfo(
      userInfo: UserInfo,
      createByUserInfo: (UserInfo) ⇒ DbResultT[M])(implicit ec: EC, db: DB, ac: AC): DbResultT[(M, Account)] =
    for {
      user    ← * <~ findByEmail(userInfo.email).findOrCreate(createByUserInfo(userInfo))
      account ← * <~ findAccount(user)
    } yield (user, account)

  /*
    1. Exchange code to access token
    2. Get base user info: email and name
    3. FindOrCreate<UserModel>
    4. respondWithToken
   */
  def oauthCallback(
      oauthResponse: OauthCallbackResponse,
      createByUserInfo: (UserInfo) ⇒ DbResultT[M])(implicit ec: EC, db: DB, ac: AC): DbResultT[Token] =
    for {
      info        ← * <~ fetchUserInfoFromCode(oauthResponse)
      userAccount ← * <~ findOrCreateUserFromInfo(info, createByUserInfo)
      (user, account) = userAccount
      token ← * <~ createToken(user, account, info)
    } yield token

  def customerCallback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    commonCallback(createCustomerByUserInfo, Uri./)(oauthResponse)

  def adminCallback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    commonCallback(createAdminByUserInfo, Uri("/admin"))(oauthResponse)

  private def commonCallback(createByUserInfo: UserInfo ⇒ DbResultT[M], redirectUri: Uri)(
      oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    // TODO: rethink discarding warnings here @michalrus
    onSuccess(oauthCallback(oauthResponse, createByUserInfo).runDBIO.runEmptyA.value) { tokenOrFailure ⇒
      tokenOrFailure
        .flatMap(Authenticator.oauthTokenLoginResponse(redirectUri))
        .fold({ f ⇒
          complete(renderFailure(f))
        }, identity)
    }
}
