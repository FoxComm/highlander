package phoenix.services.auth

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import cats.implicits._
import com.pellucid.sealerate
import core.db._
import core.failures.{Failures, GeneralFailure}
import phoenix.failures.OauthFailures._
import phoenix.failures.UserFailures.{OrganizationNotFound, OrganizationNotFoundWithDomain}
import phoenix.libs.oauth.{FacebookProvider, GoogleProvider, Oauth, OauthProvider, UserInfo}
import phoenix.models.account._
import phoenix.models.auth.{Token, UserToken}
import phoenix.payloads.CustomerPayloads.CreateCustomerPayload
import phoenix.payloads.StoreAdminPayloads.CreateStoreAdminPayload
import phoenix.services.account.{AccountCreateContext, AccountManager}
import phoenix.services.customers.CustomerManager
import phoenix.services.{Authenticator, StoreAdminManager}
import phoenix.utils.FoxConfig.{config, OauthProviderName}
import phoenix.utils.aliases._
import phoenix.utils.http.Http._
import phoenix.utils.{ADT, FoxConfig}

case class OauthCallbackResponse(code: Option[String] = None, error: Option[String] = None) {

  def getCode: Either[Failures, String] =
    (error, code) match {
      case (None, Some(providedCode)) ⇒ Either.right(providedCode)
      case (Some(err), _)             ⇒ Either.left(CallbackResponseError(err).single)
      case (_, None)                  ⇒ Either.left(CallbackResponseError("code is empty").single)
    }
}

object OauthDirectives {

  def oauthResponse: Directive1[OauthCallbackResponse] =
    parameterMap.map { params ⇒
      OauthCallbackResponse(code = params.get("code"), error = params.get("error"))
    }
}

trait OauthService { this: Oauth ⇒

  def createCustomerByUserInfo(userInfo: UserInfo)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] = {
    val serviceConfig = config.users.customer
    val context = AccountCreateContext(roles = List(serviceConfig.role),
                                       org = serviceConfig.org,
                                       scopeId = serviceConfig.scopeId)

    val payload = CreateCustomerPayload(email = userInfo.email, name = userInfo.name.some)

    for {
      result ← * <~ CustomerManager.create(payload, admin = None, context = context)
      (response, _) = result
      user ← * <~ Users.mustFindByAccountId(response.id)
    } yield user
  }

  def createAdminByUserInfo(userInfo: UserInfo)(implicit ec: EC, db: DB, ac: AC): DbResultT[User] =
    for {
      domain ← * <~ userInfo.emailDomain
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
                                        roles = List(config.users.admin.role),
                                        org = organization.name)
      response ← * <~ StoreAdminManager.create(payload, author = None)
      user     ← * <~ Users.mustFindByAccountId(response.id)

    } yield user

  def createToken(user: User, account: Account, userInfo: UserInfo, defaultScopeId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[Token] =
    for {
      domain         ← * <~ userInfo.emailDomain
      scopeDomainOpt ← * <~ ScopeDomains.findByDomain(domain).one
      scope ← * <~ scopeDomainOpt.map { scopeDomain ⇒
               Scopes.mustFindById404(scopeDomain.scopeId)
             }
      claims ← * <~ AccountManager.getClaims(account.id, scope.map(_.id).getOrElse(defaultScopeId))
      token  ← * <~ UserToken.fromUserAccount(user, account, claims)
    } yield token

  /*
    1. Exchange code to access token
    2. Get base user info: email and name
   */
  def fetchUserInfoFromCode(oauthResponse: OauthCallbackResponse)(implicit ec: EC,
                                                                  db: DB): DbResultT[UserInfo] =
    for {
      code ← * <~ oauthResponse.getCode
      accessTokenResp ← * <~ this
                         .accessToken(code)
                         .leftMap(t ⇒ GeneralFailure(t.toString).single)
                         .value
      info ← * <~ this
              .userInfo(accessTokenResp.access_token)
              .leftMap(t ⇒ GeneralFailure(t.toString).single)
              .value
    } yield info

  def findOrCreateUserFromInfo(userInfo: UserInfo, createByUserInfo: (UserInfo) ⇒ DbResultT[User])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[(User, Account)] =
    for {
      user    ← * <~ Users.findByEmail(userInfo.email).one.findOrCreate(createByUserInfo(userInfo))
      account ← * <~ Accounts.mustFindById404(user.accountId)
    } yield (user, account)

  /*
    1. Exchange code to access token
    2. Get base user info: email and name
    3. FindOrCreate<UserModel>
    4. respondWithToken
   */
  private def oauthCallback(oauthResponse: OauthCallbackResponse,
                            createByUserInfo: (UserInfo) ⇒ DbResultT[User],
                            defaultScopeId: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Token] =
    for {
      info        ← * <~ fetchUserInfoFromCode(oauthResponse)
      userAccount ← * <~ findOrCreateUserFromInfo(info, createByUserInfo)
      (user, account) = userAccount
      token ← * <~ createToken(user, account, info, defaultScopeId)
    } yield token

  def callback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route

  protected def customerCallback(
      oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    commonCallback(createCustomerByUserInfo, Uri./, config.users.customer.scopeId)(oauthResponse)

  protected def adminCallback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    commonCallback(createAdminByUserInfo, Uri("/admin"), config.users.admin.scopeId)(oauthResponse)

  private def commonCallback(
      createByUserInfo: UserInfo ⇒ DbResultT[User],
      redirectUri: Uri,
      defaultScopeId: Int)(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    // TODO: rethink discarding warnings here @michalrus
    onSuccess(oauthCallback(oauthResponse, createByUserInfo, defaultScopeId).runDBIO.runEmptyA.value) {
      tokenOrFailure ⇒
        tokenOrFailure
          .flatMap(Authenticator.oauthTokenLoginResponse(redirectUri))
          .fold({ f ⇒
            complete(renderFailure(f))
          }, identity)
    }
}

trait AdminOauthService { self: OauthService ⇒
  def callback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    adminCallback(oauthResponse)
}

trait CustomerOauthService { self: OauthService ⇒
  def callback(oauthResponse: OauthCallbackResponse)(implicit ec: EC, db: DB, ac: AC): Route =
    customerCallback(oauthResponse)
}

object OauthService {
  type OauthServiceImpl = Oauth with OauthService with OauthProvider

  trait PhoenixOauthService {
    def customer: OauthServiceImpl
    def admin: OauthServiceImpl
  }

  sealed trait OauthUserType
  implicit object OauthUserType extends ADT[OauthUserType] {
    case object Admin    extends OauthUserType
    case object Customer extends OauthUserType

    def types = sealerate.values[OauthUserType]
  }
}

object OauthServices extends FoxConfig.SupportedOauthProviders[OauthService.PhoenixOauthService] {

  import OauthService._

  def apply(providerName: OauthProviderName, userType: OauthUserType)(implicit ec: EC): OauthServiceImpl = {
    val provider = providerName match {
      case OauthProviderName.Facebook ⇒
        facebook
      case OauthProviderName.Google ⇒
        google
    }
    userType match {
      case OauthUserType.Admin ⇒
        provider.admin
      case OauthUserType.Customer ⇒
        provider.customer
    }
  }

  lazy val google: PhoenixOauthService = new PhoenixOauthService {
    def customer: OauthServiceImpl =
      googleOauthServiceFromConfig(config.users.customer, OauthUserType.Customer)

    def admin: OauthServiceImpl =
      googleOauthServiceFromConfig(config.users.admin, OauthUserType.Admin)
  }

  lazy val facebook: PhoenixOauthService = new PhoenixOauthService {
    def customer: OauthServiceImpl =
      fbOauthService(config.users.customer, OauthUserType.Customer)

    def admin: OauthServiceImpl =
      fbOauthService(config.users.admin, OauthUserType.Admin)
  }

  private def googleOauthServiceFromConfig(configUser: FoxConfig.User, userType: OauthUserType) =
    userType match {
      case OauthUserType.Admin ⇒
        new Oauth(configUser.oauth.google) with GoogleProvider with OauthService with AdminOauthService
      case OauthUserType.Customer ⇒
        new Oauth(configUser.oauth.google) with GoogleProvider with OauthService with CustomerOauthService
    }

  private def fbOauthService(configUser: FoxConfig.User, userType: OauthUserType) =
    userType match {
      case OauthUserType.Admin ⇒
        new Oauth(configUser.oauth.facebook) with FacebookProvider with OauthService with AdminOauthService
      case OauthUserType.Customer ⇒
        new Oauth(configUser.oauth.facebook) with FacebookProvider with OauthService with CustomerOauthService
    }

}
