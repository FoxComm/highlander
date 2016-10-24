package services.auth

import failures.UserFailures._
import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.account._
import models.auth.{Token, UserToken}
import services.account._
import utils.FoxConfig._
import utils.aliases._
import utils.db._

class GoogleOauthUser(options: GoogleOauthOptions)(implicit ec: EC, db: DB)
    extends Oauth(options)
    with OauthService[User]
    with GoogleProvider {

  def getScopeId: Int = options.scopeId

  def createByUserInfo(userInfo: UserInfo): DbResultT[User] = {

    for {
      scope ← * <~ Scopes.mustFindById404(options.scopeId)
      organization ← * <~ Organizations
                      .findByNameInScope(options.orgName, scope.id)
                      .mustFindOr(OrganizationNotFound(options.orgName, scope.path))
      role ← * <~ Roles
              .findByNameInScope(options.roleName, scope.id)
              .mustFindOr(RoleNotFound(options.roleName, scope.path))

      account ← * <~ Accounts.create(Account())
      user ← * <~ Users.create(
                User(accountId = account.id,
                     email = Some(userInfo.email),
                     name = Some(userInfo.name),
                     scope = Scope.empty))

      _ ← * <~ AccountOrganizations.create(
             AccountOrganization(accountId = account.id, organizationId = organization.id))

      _ ← * <~ AccountRoles.create(AccountRole(accountId = account.id, roleId = role.id))
    } yield user
  }

  def findByEmail(email: String) = Users.findByEmail(email).one

  def createToken(user: User, account: Account, scopeId: Int): DbResultT[Token] =
    for {
      claims ← * <~ AccountManager.getClaims(account.id, scopeId)
      token  ← * <~ UserToken.fromUserAccount(user, account, claims)
    } yield token

  def findAccount(user: User): DbResultT[Account] = Accounts.mustFindById404(user.accountId)
}

object GoogleOauth {

  def oauthServiceFromConfig(configPrefix: String)(implicit ec: EC, db: DB) = {

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
