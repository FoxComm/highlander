package phoenix.services

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCookie, RawHeader}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.CookieDirectives.setCookie
import akka.http.scaladsl.server.directives.RespondWithDirectives.respondWithHeader
import akka.http.scaladsl.server.directives.SecurityDirectives.AuthenticationResult
import akka.http.scaladsl.server.directives.{AuthenticationDirective, AuthenticationResult}
import cats.implicits._
import core.db._
import core.failures._
import phoenix.failures.AuthFailures._
import phoenix.models.account._
import phoenix.models.admin._
import phoenix.models.auth._
import phoenix.payloads.{AuthPayload, LoginPayload}
import phoenix.services.account._
import phoenix.services.customers.CustomerManager
import phoenix.utils.FoxConfig.config
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication

object FailureChallenge {
  def apply(realm: String, failures: Failures, scheme: String = "xBasic"): HttpChallenge =
    HttpChallenge(scheme = scheme,
                  realm = realm,
                  params = Map("error" → failures.flatten.mkString))
}

object AuthRejections {

  def credentialsMissing[T](challenge: HttpChallenge): Directive1[T] = {
    val rejection = AuthenticationFailedRejection(CredentialsMissing, challenge)
    reject(rejection)
  }

  def credentialsRejected[T](challenge: HttpChallenge): Directive1[T] = {
    val rejection = AuthenticationFailedRejection(CredentialsRejected, challenge)
    reject(rejection)
  }
}

object JwtCookie {
  def apply(authPayload: AuthPayload): HttpCookie = HttpCookie(
      name = "JWT",
      value = authPayload.jwt,
      secure = config.auth.cookie.secure,
      httpOnly = true,
      expires = config.auth.cookie.ttl.map { ttl ⇒
        DateTime.now + ttl * 1000
      },
      path = Some("/"),
      domain = config.auth.cookie.domain
  )
}

object Authenticator {
  type EmailFinder[M] = String ⇒ DBIO[Option[M]]

  //parameterized for future Service accounts
  case class AuthData[M](token: Token, model: M, account: Account, isGuest: Boolean = false)

  trait UserAuthenticator {
    def readCredentials(): Directive1[Option[String]]
    def checkAuthUser(credentials: Option[String]): Future[AuthenticationResult[AuthData[User]]]
    def checkAuthCustomer(
        credentials: Option[String]): Future[AuthenticationResult[AuthData[User]]]
  }

  class JwtAuthenticator(guestCreateContext: AccountCreateContext)(implicit ec: EC, db: DB)
      extends UserAuthenticator {

    def readCredentials(): Directive1[Option[String]] = {
      readCookieOrHeader(headerName = "JWT")
    }

    def toUserToken(token: String): Either[Failures, Token] =
      Token.fromString(token, Identity.User)

    def checkAuthUser(credentials: Option[String]): Future[AuthenticationResult[AuthData[User]]] =
      jwtAuthUser("user")(credentials)

    def checkAuthCustomer(
        credentials: Option[String]): Future[AuthenticationResult[AuthData[User]]] =
      credentials match {
        case None ⇒ jwtAuthGuest("customer")
        case _    ⇒ jwtAuthUser("customer")(credentials)
      }

    def jwtAuthUser(realm: String)(credentials: Option[String])(
        implicit ec: EC,
        db: DB): Future[AuthenticationResult[AuthData[User]]] =
      (for {
        jwtCredentials ← * <~ credentials.toEither(AuthFailed("missing credentials").single)
        token          ← * <~ toUserToken(jwtCredentials)
        account ← * <~ Accounts
                   .findByIdAndRatchet(token.id, token.ratchet)
                   .mustFindOr(AuthFailed("account not found"))
        user ← * <~ Users.mustFindByAccountId(token.id)
      } yield
        AuthData[User](token, user, account)).runDBIO.runEmptyA.value.map { // TODO: rethink discarding warnings here @michalrus
        case Right(data) ⇒ AuthenticationResult.success(data)
        case Left(f)     ⇒ AuthenticationResult.failWithChallenge(FailureChallenge(realm, f))
      }

    def jwtAuthGuest(realm: String)(implicit ec: EC,
                                    db: DB): Future[AuthenticationResult[AuthData[User]]] = {
      (for {
        guest ← * <~ CustomerManager.createGuest(guestCreateContext)
        (user, custData) = guest
        account ← * <~ Accounts.mustFindById404(user.accountId)
        claims  ← * <~ AccountManager.getClaims(user.accountId, guestCreateContext.scopeId)
      } yield
        AuthData[User](
            UserToken.fromUserAccount(user, account, claims),
            user,
            account,
            isGuest = true)).runDBIO.runEmptyA.value.map { // TODO: rethink discarding warnings here @michalrus
        case Right(data) ⇒ AuthenticationResult.success(data)
        case Left(f)     ⇒ AuthenticationResult.failWithChallenge(FailureChallenge(realm, f))
      }
    }
  }

  def forUser(guestCreateContext: AccountCreateContext)(implicit ec: EC,
                                                        db: DB): JwtAuthenticator =
    new JwtAuthenticator(guestCreateContext)

  private def readCookie(): Directive1[Option[String]] = {
    optionalCookie("JWT").map(_.map(_.value))
  }

  private def readHeader(name: String): Directive1[Option[String]] = {
    optionalHeaderValueByName(name)
  }

  private def readCookieOrHeader(headerName: String): Directive1[Option[String]] = {
    readCookie().flatMap(_.fold(readHeader(headerName))(v ⇒ provide(Some(v))))
  }

  //TODO
  //This will be replaced with claims specific require functions for admins inside
  //the services instead of at the route. We are simply checking for a role called
  //"admin" within the token. This is to bring back feature parity with the old code.
  val ADMIN_ROLE = "admin"

  def requireAdminAuth(auth: UserAuthenticator): AuthenticationDirective[AuthData[User]] = {
    (for {
      optCreds ← auth.readCredentials()
      result   ← onSuccess(auth.checkAuthUser(optCreds))
    } yield (result, optCreds)).tflatMap {
      case (Right(authData), _) ⇒ {
        if (authData.token.hasRole(ADMIN_ROLE)) provide(authData)
        else
          AuthRejections.credentialsRejected[AuthData[User]](
              FailureChallenge("admin", AuthFailed("Does not have admin role").single))
      }
      case (Left(challenge), Some(creds)) ⇒
        AuthRejections.credentialsRejected[AuthData[User]](challenge)
      case (Left(challenge), _) ⇒
        AuthRejections.credentialsMissing[AuthData[User]](challenge)
    }
  }

  //TODO
  //same as above, should have check for claims. The services should
  //make sure the claim exists instead of route layer.
  def requireCustomerAuth(auth: UserAuthenticator): AuthenticationDirective[AuthData[User]] =
    (for {
      optCreds ← auth.readCredentials()
      result   ← onSuccess(auth.checkAuthCustomer(optCreds))
    } yield (result, optCreds)).tflatMap {
      case (Right(authData), _) ⇒
        if (!authData.isGuest) provide(authData)
        else {
          Console.out.println(s"AUTH ${authData}")
          AuthPayload(
              token = UserToken.fromUserAccount(
                  authData.model,
                  authData.account,
                  Account.ClaimSet(scope = authData.token.scope,
                                   roles = authData.token.roles,
                                   claims = authData.token.claims))) match {
            case Right(authPayload) ⇒
              val header = respondWithHeader(RawHeader("JWT", authPayload.jwt))
              val cookie = setCookie(JwtCookie(authPayload))
              header & cookie & provide(authData)
            case Left(failures) ⇒
              val challenge = FailureChallenge("customer", failures)
              AuthRejections.credentialsRejected[AuthData[User]](challenge)
          }
        }
      case (Left(challenge), Some(creds)) ⇒
        AuthRejections.credentialsRejected[AuthData[User]](challenge)
      case (Left(challenge), _) ⇒
        AuthRejections.credentialsMissing[AuthData[User]](challenge)
    }

  def authTokenBaseResponse(token: Token,
                            response: AuthPayload ⇒ StandardRoute): Either[Failures, Route] = {
    for {
      authPayload ← AuthPayload(token)
    } yield
      respondWithHeader(RawHeader("JWT", authPayload.jwt)).&(setCookie(JwtCookie(authPayload))) {
        response(authPayload)
      }
  }

  def authTokenLoginResponse(token: Token): Either[Failures, Route] = {
    authTokenBaseResponse(token, { payload ⇒
      complete(
          HttpResponse(
              entity = HttpEntity(ContentTypes.`application/json`, payload.claims.toJson)))
    })
  }

  def oauthTokenLoginResponse(redirectUri: Uri)(token: Token): Either[Failures, Route] = {
    authTokenBaseResponse(token, { _ ⇒
      redirect(redirectUri, StatusCodes.Found)
    })
  }

  def authenticate(payload: LoginPayload)(implicit ec: EC, db: DB): Result[Route] = {
    val tokenResult = for {
      organization ← * <~ Organizations.findByName(payload.org).mustFindOr(LoginFailed)
      user         ← * <~ Users.findNonGuestByEmail(payload.email.toLowerCase).mustFindOneOr(LoginFailed)
      _            ← * <~ user.mustNotBeMigrated
      accessMethod ← * <~ AccountAccessMethods
                      .findOneByAccountIdAndName(user.accountId, "login")
                      .mustFindOr(LoginFailed)
      account ← * <~ Accounts.mustFindById404(user.accountId)

      // security checks
      _ ← * <~ failIfNot(accessMethod.checkPassword(payload.password), LoginFailed)

      adminUsers ← * <~ AdminsData.findOneByAccountId(user.accountId)
      _ ← * <~ adminUsers.map { adminData ⇒
           DbResultT.fromEither(checkState(adminData))
         }

      claimSet     ← * <~ AccountManager.getClaims(account.id, organization.scopeId)
      _            ← * <~ validateClaimSet(claimSet)
      checkedToken ← * <~ UserToken.fromUserAccount(user, account, claimSet)
    } yield checkedToken

    for {
      token ← tokenResult.runDBIO
      route ← Result.fromEither(authTokenLoginResponse(token))
    } yield route
  }

  //A user must have at least some role in an organization to login under that
  //organization. Otherwise they get an auth failure.
  private def validateClaimSet(claimSet: Account.ClaimSet): Either[Failures, Unit] =
    if (claimSet.roles.isEmpty)
      Either.left(AuthFailed(reason = "User has no roles in the organization").single)
    else
      Either.right(Unit)

  private def checkState(adminData: AdminData): Either[Failures, AdminData] = {
    if (adminData.canLogin) Either.right(adminData)
    else Either.left(AuthFailed(reason = "Store admin is Inactive or Archived").single)
  }
}
