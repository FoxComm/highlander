package services.auth

import services.account._
import services.customers.CustomerManager
import payloads.CustomerPayloads._
import payloads.StoreAdminPayloads._
import responses.CustomerResponse._
import services.StoreAdminManager
import cats.implicits._
import libs.oauth.{GoogleOauthOptions, GoogleProvider, Oauth, UserInfo}
import models.auth.{Token, UserToken}
import models.account._
import failures.UserFailures._
import utils.FoxConfig
import utils.FoxConfig.config
import utils.aliases._
import utils.db._

class GoogleOauthUser(options: GoogleOauthOptions)(implicit ec: EC, db: DB, ac: AC)
    extends Oauth(options)
    with OauthService[User]
    with GoogleProvider {

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

  private def extractUserDomain(userInfo: UserInfo): String = {
    val userAtDomain = userInfo.email.split("@")
    if (userAtDomain.size == 2) userAtDomain(1) else ""
  }

  def createAdminByUserInfo(userInfo: UserInfo): DbResultT[User] = {
    val domain = extractUserDomain(userInfo)

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

  def createToken(user: User, account: Account, userInfo: UserInfo): DbResultT[Token] =
    for {
      domain         ← * <~ extractUserDomain(userInfo)
      scopeDomainOpt ← * <~ ScopeDomains.findByDomain(domain).one
      scope ← * <~ scopeDomainOpt.map { scopeDomain ⇒
               Scopes.mustFindById404(scopeDomain.scopeId)
             }
      claims ← * <~ AccountManager.getClaims(account.id,
                                             scope.map(_.id).getOrElse(options.scopeId))
      token ← * <~ UserToken.fromUserAccount(user, account, claims)
    } yield token

  def findAccount(user: User): DbResultT[Account] = Accounts.mustFindById404(user.accountId)
}

object GoogleOauth {

  def oauthServiceFromConfig(configUser: FoxConfig.User)(implicit ec: EC, db: DB, ac: AC) = {

    val opts = GoogleOauthOptions(roleName = configUser.role,
                                  orgName = configUser.org,
                                  scopeId = configUser.scopeId,
                                  clientId = configUser.oauth.google.clientId,
                                  clientSecret = configUser.oauth.google.clientSecret,
                                  redirectUri = configUser.oauth.google.redirectUri,
                                  hostedDomain = configUser.oauth.google.hostedDomain)

    new GoogleOauthUser(opts)
  }
}