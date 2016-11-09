package services.auth

import services.account._
import services.customers.CustomerManager
import payloads.CustomerPayloads._
import payloads.StoreAdminPayloads._
import responses.CustomerResponse._
import services.StoreAdminManager
import cats.implicits._
import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.auth.{UserToken, Token}
import models.account._
import failures.UserFailures._
import utils.FoxConfig._
import utils.aliases._
import utils.db._

class GoogleOauthUser(options: GoogleOauthOptions)(implicit ec: EC, db: DB, ac: AC)
    extends Oauth(options)
    with OauthService[User]
    with GoogleProvider {

  def getScopeId: Int = options.scopeId

  def createCustomerByUserInfo(userInfo: UserInfo): DbResultT[User] = {
    val context = AccountCreateContext(roles = List(options.roleName),
                                       org = options.orgName,
                                       scopeId = options.scopeId)

    val payload = CreateCustomerPayload(email = userInfo.email, name = userInfo.name.some)

    for {
      result ← * <~ CustomerManager.create(payload, admin = None, context = context)
      (response, auth) = result
      user ← * <~ Users.mustFindByAccountId(response.id)
    } yield user
  }

  def createAdminByUserInfo(userInfo: UserInfo): DbResultT[User] = {

    val userAtDomain = userInfo.email.split("@")
    val domain       = if (userAtDomain.size == 2) userAtDomain(1) else ""

    for {

      //We must determine the org based on the domain of the user email
      scopeDomain ← * <~ ScopeDomains
                     .findByDomain(domain)
                     .mustFindOneOr(OrganizationNotFoundWithDomain(domain))
      scope ← * <~ Scopes.mustFindById404(scopeDomain.scopeId)
      organization ← * <~ Organizations
                      .findByScopeId(scopeDomain.scopeId)
                      .mustFindOr(OrganizationNotFound(domain, scope.path))

      payload = CreateStoreAdminPayload(email = userInfo.email,
                                        name = userInfo.name,
                                        password = None,
                                        roles = List(options.roleName),
                                        org = organization.name)
      response ← * <~ StoreAdminManager.create(payload, author = None)
      user     ← * <~ Users.mustFindByAccountId(response.id)

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

  def oauthServiceFromConfig(configPrefix: String)(implicit ec: EC, db: DB, ac: AC) = {

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
