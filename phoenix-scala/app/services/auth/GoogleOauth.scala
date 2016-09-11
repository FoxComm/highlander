package services.auth

import cats.implicits._
import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.auth.{AdminToken, CustomerToken, Identity, Token}
import models.account._
import utils.FoxConfig._
import utils.aliases._
import utils.db._

class GoogleOauthStoreAdmin(options: GoogleOauthOptions)
    extends Oauth(options)
    with OauthService[User]
    with GoogleProvider {

  def createByUserInfo(userInfo: UserInfo)(implicit ec: EC): DbResultT[User] =
    for { 
        account ← * <~ Accounts.create(Account())
        user ← * <~ Users.create(
          Users(
            accountId = account.id, 
            email = userInfo.email, 
            name = userInfo.name))
        //MAXDO Assign merchant_admin role
        // Also verify domain of merchant org here here?
    } yield user

  def findByEmail(email: String)(implicit ec: EC, db: DB) = Users.findByEmail(email)

  def createToken(admin: User): Token = AccountToken.fromAdmin(admin)
}

class GoogleOauthCustomer(options: GoogleOauthOptions)
    extends Oauth(options)
    with OauthService[User]
    with GoogleProvider {

  def createByUserInfo(userInfo: UserInfo)(implicit ec: EC): DbResultT[User] =
    for {
        account ← * <~ Accounts.create(Account())
        user ← * <~ Users.create(
          Users(
            accountId = account.id, 
            email = userInfo.email, 
            name = userInfo.name))
        //MAXDO Assign customer role
    } yield user

  def findByEmail(email: String)(implicit ec: EC, db: DB) = Users.findByEmail(email)

  def createToken(user: User): Token = AccountToken.fromUser(customer)
}

object GoogleOauth {

  def oauthServiceFromConfig(identity: Identity.IdentityKind) = {
    val configPrefix = identity.toString.toLowerCase

    val opts = GoogleOauthOptions(
        clientId = config.getString(s"oauth.$configPrefix.google.client_id"),
        clientSecret = config.getString(s"oauth.$configPrefix.google.client_secret"),
        redirectUri = config.getString(s"oauth.$configPrefix.google.redirect_uri"),
        hostedDomain = config.getOptString(s"oauth.$configPrefix.google.hosted_domain"))

    identity match {
      case Identity.Customer ⇒
        new GoogleOauthCustomer(opts)
      case Identity.Admin ⇒
        new GoogleOauthStoreAdmin(opts)
      case _ ⇒
        throw new RuntimeException(s"Identity $configPrefix not supported for google oauth.")
    }
  }
}
