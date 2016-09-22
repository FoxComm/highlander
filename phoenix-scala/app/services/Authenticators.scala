package services

import scala.concurrent.Future
import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCookie, HttpCredentials, RawHeader}
import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.CookieDirectives.setCookie
import akka.http.scaladsl.server.directives.RespondWithDirectives.respondWithHeader
import akka.http.scaladsl.server.directives.SecurityDirectives.AuthenticationResult
import akka.http.scaladsl.server.directives.{AuthenticationDirective, AuthenticationResult}

import cats.data.Xor
import failures.AuthFailures._
import failures.Failures
import models.auth.{Identity, _}
import services.account._
import services.customers.CustomerManager
import models.account._
import models.admin._
import org.jose4j.jwt.JwtClaims
import payloads.LoginPayload
import slick.driver.PostgresDriver.api._
import utils.FoxConfig.{RichConfig, config}
import utils.Passwords.checkPassword
import utils.aliases._
import utils.db._

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication

case class AuthPayload(claims: JwtClaims, jwt: String)

object AuthPayload {
  def apply(token: Token): Failures Xor AuthPayload = {
    val claims = Token.getJWTClaims(token)
    Token.encodeJWTClaims(claims).map { encoded ⇒
      AuthPayload(claims = claims, jwt = encoded)
    }
  }
}

object FailureChallenge {
  def apply(realm: String, failures: Failures, scheme: String = "Basic"): HttpChallenge =
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
      secure = config.getOptBool("auth.cookieSecure").getOrElse(true),
      httpOnly = true,
      expires = config.getOptLong("auth.cookieTTL").map { ttl ⇒
        DateTime.now + ttl * 1000
      },
      path = Some("/"),
      domain = config.getOptString("auth.cookieDomain")
  )
}

object Authenticator {
  type EmailFinder[M] = String ⇒ DBIO[Option[M]]

  //parameterized for future Service accounts
  case class AuthData[M](model: M, account: Account, scope: String, claims: Account.Claims)

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

    def toUserToken(token: String): Failures Xor Token =
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
        jwtCredentials ← * <~ credentials.toXor(AuthFailed("missing credentials").single)
        token          ← * <~ toUserToken(jwtCredentials)
        account ← * <~ Accounts
                   .findByIdAndRatchet(token.id, token.ratchet)
                   .mustFindOr(AuthFailed("account not found"))
        user ← * <~ Users.mustFindByAccountId(token.id)
      } yield AuthData[User](user, account, token.scope, token.claims)).run().map {
        case Xor.Right(data) ⇒ AuthenticationResult.success(data)
        case Xor.Left(f)     ⇒ AuthenticationResult.failWithChallenge(FailureChallenge(realm, f))
      }

    def jwtAuthGuest(realm: String)(implicit ec: EC,
                                    db: DB): Future[AuthenticationResult[AuthData[User]]] = {
      (for {
        guest ← * <~ CustomerManager.createGuest(guestCreateContext)
        (user, custUser) = guest
        account     ← * <~ Accounts.mustFindById404(user.accountId)
        claimResult ← * <~ AccountManager.getClaims(user.accountId, guestCreateContext.scopeId)
        (scope, claims) = claimResult
      } yield AuthData[User](user, account, scope, claims)).run().map {
        case Xor.Right(data) ⇒ AuthenticationResult.success(data)
        case Xor.Left(f)     ⇒ AuthenticationResult.failWithChallenge(FailureChallenge(realm, f))
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

  //MAXDO
  //This will be replaced with claims specific require functions for admins inside
  //the services instead of at the route later
  def requireAdminAuth(auth: UserAuthenticator): AuthenticationDirective[User] = {
    (for {
      optCreds ← auth.readCredentials()
      result   ← onSuccess(auth.checkAuthUser(optCreds))
    } yield (result, optCreds)).tflatMap {
      case (Right(authData), _) ⇒ provide(authData.model)
      case (Left(challenge), Some(creds)) ⇒
        AuthRejections.credentialsRejected[User](challenge)
      case (Left(challenge), _) ⇒
        AuthRejections.credentialsMissing[User](challenge)
    }
  }

  //MAXDO
  //same as above, should have check for claims. The services should
  //make sure the claim exists instead of route layer.
  def requireCustomerAuth(auth: UserAuthenticator): AuthenticationDirective[User] =
    (for {
      optCreds ← auth.readCredentials()
      result   ← onSuccess(auth.checkAuthCustomer(optCreds))
    } yield (result, optCreds)).tflatMap {
      case (Right(authData), _) ⇒
        AuthPayload(
            token = UserToken.fromUserAccount(authData.model,
                                              authData.account,
                                              authData.scope,
                                              authData.claims)) match {
          case Xor.Right(authPayload) ⇒
            val header = respondWithHeader(RawHeader("JWT", authPayload.jwt))
            val cookie = setCookie(JwtCookie(authPayload))
            header & cookie & provide(authData.model)
          case Xor.Left(failures) ⇒
            val challenge = FailureChallenge("customer", failures)
            AuthRejections.credentialsRejected[User](challenge)
        }
      case (Left(challenge), Some(creds)) ⇒
        AuthRejections.credentialsRejected[User](challenge)
      case (Left(challenge), _) ⇒
        AuthRejections.credentialsMissing[User](challenge)
    }

  def authTokenBaseResponse(token: Token,
                            response: AuthPayload ⇒ StandardRoute): Failures Xor Route = {
    for {
      authPayload ← AuthPayload(token)
    } yield
      respondWithHeader(RawHeader("JWT", authPayload.jwt)).&(setCookie(JwtCookie(authPayload))) {
        response(authPayload)
      }
  }

  def authTokenLoginResponse(token: Token): Failures Xor Route = {
    authTokenBaseResponse(token, { payload ⇒
      complete(
          HttpResponse(
              entity = HttpEntity(ContentTypes.`application/json`, payload.claims.toJson)))
    })
  }

  def oauthTokenLoginResponse(token: Token): Failures Xor Route = {
    authTokenBaseResponse(token, { payload ⇒
      redirect(Uri./, StatusCodes.Found)
    })
  }

  def authenticate(payload: LoginPayload)(implicit ec: EC, db: DB): Result[Route] = {

    val tokenResult = (for {
      organization ← * <~ Organizations.findByName(payload.org).mustFindOr(LoginFailed)
      user         ← * <~ Users.findByEmail(payload.email).mustFindOneOr(LoginFailed)
      accessMethod ← * <~ AccountAccessMethods
                      .findOneByAccountIdAndName(user.accountId, "login")
                      .mustFindOr(LoginFailed)
      account ← * <~ Accounts.mustFindById404(user.accountId)

      validatedUser ← * <~ validatePassword(user,
                                            accessMethod.hashedPassword,
                                            payload.password,
                                            accessMethod.algorithm)
      //TODO Add this back after demo
      //adminUsers    ← * <~ StoreAdminUsers.filter(_.accountId === user.accountId).one
      //_             ← * <~ adminUsers.map(aus ⇒ checkState(aus))

      claimResult ← * <~ AccountManager.getClaims(account.id, organization.scopeId)
      (scope, claims) = claimResult
      checkedToken ← * <~ UserToken.fromUserAccount(validatedUser, account, scope, claims)
    } yield checkedToken).run()

    tokenResult.map(_.flatMap { token ⇒
      authTokenLoginResponse(token)
    })
  }

  private def validatePassword[User](user: User,
                                     hashedPassword: String,
                                     password: String,
                                     algorithm: Int): Failures Xor User = {

    val passwordsMatch = algorithm match {

      case 0 ⇒ checkPassword(password, hashedPassword)
      case 1 ⇒ password == hashedPassword //TODO remove , only fo demo.
    }

    if (passwordsMatch)
      Xor.right(user)
    else
      Xor.left(LoginFailed.single)
  }

  private def checkState(storeAdminUser: StoreAdminUser): Failures Xor StoreAdminUser = {
    if (storeAdminUser.canLogin) Xor.right(storeAdminUser)
    else Xor.left(AuthFailed(reason = "Store admin is Inactive or Archived").single)
  }
}
