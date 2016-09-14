package services.auth

import cats.implicits._
import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.auth.{UserToken, Token}
import models.account._
import utils.FoxConfig._
import utils.aliases._
import utils.db._

class GoogleOauthUser(options: GoogleOauthOptions)
    extends Oauth(options)
    with OauthService[User]
    with GoogleProvider {

  def createByUserInfo(userInfo: UserInfo)(implicit ec: EC): DbResultT[User] = {

    for {
      scope        ← * <~ Scopes.mustFindById404(options.scopeId)
      organization ← * <~ Organizations.findByNameInScope(options.orgName, scope.id);
      role         ← * <~ Roles.findByNameInScope(options.roleName, scope.id);

      account ← * <~ Accounts.create(Account())
      user ← * <~ Users.create(
                Users(accountId = account.id, email = userInfo.email, name = userInfo.name))

      _ ← * <~ AccountOrganizations.create(
             AccountOrganization(accountId = account.id, organizationId = organization.id))

      _ ← * <~ AccountRoles.create(AccountRole(accountId = account.id, roleId = role.id))
    } yield user
  }

  def findByEmail(email: String)(implicit ec: EC, db: DB) = Users.findByEmail(email)

  def createToken(user: User, account: Account, scope: String, claims: Account.Claims): Token =
    UserToken.fromUserAccount(user, account, scope, claims)
}

object GoogleOauth {

  def oauthServiceFromConfig(configPrefix: String) = {

    val opts = GoogleOauthOptions(
        roleName = config.getString(s"user.$configPrefix.role"),
        orgName = config.getString(s"user.$configPrefix.org"),
        scopeId = config.getInt(s"user.$configPrefix.scope_id"),
        clientId = config.getString(s"oauth.$configPrefix.google.client_id"),
        clientSecret = config.getString(s"oauth.$configPrefix.google.client_secret"),
        redirectUri = config.getString(s"oauth.$configPrefix.google.redirect_uri"),
        hostedDomain = config.getOptString(s"oauth.$configPrefix.google.hosted_domain"))

    new GoogleOauthUser(opts)
  }
}
