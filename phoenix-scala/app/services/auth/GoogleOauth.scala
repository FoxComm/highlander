package services.auth

import cats.implicits._
import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.auth.{UserToken, Identity, Token}
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
      scope        ← * <~ Scopes.mustFindById404(userInfo.scopeId)
      organization ← * <~ Organizations.findByNameInScope(orgName, scope.id);
      role         ← * <~ Roles.findByNameInScope(userInfo.roleName, scope.id);

      account ← * <~ Accounts.create(Account())
      user ← * <~ Users.create(
                Users(accountId = account.id, email = userInfo.email, name = userInfo.name))

      _ ← * <~ AccountOrganizations.create(
             AccountOrganization(accountId = account.id, organizationId = organization.id))

      _ ← * <~ AccountRoles.create(AccountRole(accountId = account.id, roleId = role.id))
    } yield user
  }

  def findByEmail(email: String)(implicit ec: EC, db: DB) = Users.findByEmail(email)

  def createToken(user: User): Token = UserToken.fromUser(user)
}

object GoogleOauth {

  def oauthServiceFromConfig(identity: Identity.IdentityKind) = {
    val configPrefix = identity.toString.toLowerCase

    val opts = GoogleOauthOptions(
        roleName = config.getString(s"oauth.$configPrefix.user_role"),
        orgName = config.getString(s"oauth.$configPrefix.user_org"),
        scopeId = config.getInt(s"oauth.$configPrefix.scope_id"),
        clientId = config.getString(s"oauth.$configPrefix.google.client_id"),
        clientSecret = config.getString(s"oauth.$configPrefix.google.client_secret"),
        redirectUri = config.getString(s"oauth.$configPrefix.google.redirect_uri"),
        hostedDomain = config.getOptString(s"oauth.$configPrefix.google.hosted_domain"))

    identity match {
      case Identity.Customer ⇒
        new GoogleOauthUser(opts)
      case Identity.Admin ⇒
        new GoogleOauthUser(opts)
      case Identity.Service ⇒
        throw new RuntimeException(s"Service Accounts not implemented yet.")
      case _ ⇒
        throw new RuntimeException(s"Identity $configPrefix not supported for google oauth.")
    }
  }
}
