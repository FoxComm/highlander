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
import models.customer.{Customer, Customers}
import models.{StoreAdmin, StoreAdmins}
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
  type EmailFinder[M]  = String ⇒ DBIO[Option[M]]
  type TokenToModel[M] = Token ⇒ Failures Xor DBIO[Option[M]]

  trait AsyncAuthenticator[M] {
    type C
    def readCredentials(): Directive1[Option[C]]
    def checkAuth(creds: Option[C]): Future[AuthenticationResult[M]]
  }

  trait BasicAuth[M] extends AsyncAuthenticator[M] {
    type C = HttpCredentials

    def readCredentials(): Directive1[Option[HttpCredentials]] = {
      extractCredentials
    }

    protected def basicAuth[F <: EmailFinder[M]](realm: String)(
        credentials: Option[HttpCredentials],
        finder: F,
        getHashedPassword: M ⇒ Option[String])(implicit ec: EC,
                                               db: DB): Future[AuthenticationResult[M]] = {
      (for {
        userCredentials ← * <~ Credentials.mustVerifyBasicCredentials(
                             credentials,
                             AuthFailed("missing credentials").single)
        user      ← * <~ finder(userCredentials.identifier).mustFindOr(LoginFailed)
        validated ← * <~ validatePassword(user, getHashedPassword(user), userCredentials.secret)
      } yield validated).run().map {
        case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
        case Xor.Left(f)       ⇒ AuthenticationResult.failWithChallenge(FailureChallenge(realm, f))
      }
    }
  }

  case class BasicCustomer(implicit ec: EC, db: DB) extends BasicAuth[Customer] {
    def checkAuth(credentials: Option[HttpCredentials]): Future[AuthenticationResult[Customer]] = {
      basicAuth[EmailFinder[Customer]]("private customer routes")(credentials,
                                                                  Customers.findByEmail,
                                                                  _.hashedPassword)
    }
  }

  case class BasicStoreAdmin(implicit ec: EC, db: DB) extends BasicAuth[StoreAdmin] {
    def checkAuth(credentials: Option[HttpCredentials]): Future[AuthenticationResult[StoreAdmin]] = {
      basicAuth[EmailFinder[StoreAdmin]]("admin")(credentials,
                                                  StoreAdmins.findByEmail,
                                                  _.hashedPassword)
    }
  }

  trait JwtAuth[M] extends AsyncAuthenticator[M] {
    type C = String

    def readCredentials(): Directive1[Option[String]] = {
      readCookieOrHeader(headerName = "JWT")
    }

    def validateToken(token: String): Failures Xor Token

    protected def jwtAuth[F <: TokenToModel[M]](realm: String)(
        credentials: Option[String],
        userFromToken: F)(implicit ec: EC, db: DB): Future[AuthenticationResult[M]] =
      (for {
        jwtCredentials ← * <~ credentials.toXor(AuthFailed("missing credentials").single)
        token          ← * <~ validateToken(jwtCredentials)
        userDbio       ← * <~ userFromToken(token)
        user           ← * <~ userDbio.mustFindOr(LoginFailed)
      } yield user).run().map {
        case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
        case Xor.Left(f)       ⇒ AuthenticationResult.failWithChallenge(FailureChallenge(realm, f))
      }

    protected def jwtAuthGuest[F <: TokenToModel[Customer]](
        realm: String)(implicit ec: EC, db: DB): Future[AuthenticationResult[Customer]] =
      Customers
        .create(Customer.buildGuest())
        .fold(f ⇒ AuthenticationResult.failWithChallenge(FailureChallenge(realm, f)),
              entity ⇒ AuthenticationResult.success(entity))
        .run()
  }

  case class JwtCustomer(implicit ec: EC, db: DB) extends JwtAuth[Customer] {

    import JwtCustomer._

    def checkAuth(credentials: Option[String]): Future[AuthenticationResult[Customer]] =
      credentials match {
        case None ⇒ jwtAuthGuest[TokenToModel[Customer]](realmName)
        case _    ⇒ jwtAuth[TokenToModel[Customer]](realmName)(credentials, customerFromToken)
      }

    def validateToken(token: String) = Token.fromString(token, Identity.Customer)
  }

  object JwtCustomer {
    val realmName = "private customer routes"
  }

  case class JwtStoreAdmin(implicit ec: EC, db: DB) extends JwtAuth[StoreAdmin] {
    def checkAuth(credentials: Option[String]): Future[AuthenticationResult[StoreAdmin]] = {
      jwtAuth[TokenToModel[StoreAdmin]]("admin")(credentials, adminFromToken)
    }

    def validateToken(token: String) = Token.fromString(token, Identity.Admin)
  }

  def forAdminFromConfig(implicit ec: EC, db: DB): AsyncAuthenticator[StoreAdmin] = {
    config.getString("auth.method") match {
      case "basic" ⇒ BasicStoreAdmin()
      case "jwt"   ⇒ JwtStoreAdmin()
      case method  ⇒ throw new RuntimeException(s"unknown auth method $method")
    }
  }

  def forCustomerFromConfig(implicit ec: EC, db: DB): AsyncAuthenticator[Customer] = {
    config.getString("auth.method") match {
      case "basic" ⇒ BasicCustomer()
      case "jwt"   ⇒ JwtCustomer()
      case method  ⇒ throw new RuntimeException(s"unknown auth method $method")
    }
  }

  private def readCookie(): Directive1[Option[String]] = {
    optionalCookie("JWT").map(_.map(_.value))
  }

  private def readHeader(name: String): Directive1[Option[String]] = {
    optionalHeaderValueByName(name)
  }

  private def readCookieOrHeader(headerName: String): Directive1[Option[String]] = {
    readCookie().flatMap(_.fold(readHeader(headerName))(v ⇒ provide(Some(v))))
  }

  def requireAdminAuth(auth: AsyncAuthenticator[StoreAdmin]): AuthenticationDirective[StoreAdmin] =
    (for {
      optCreds ← auth.readCredentials()
      result   ← onSuccess(auth.checkAuth(optCreds))
    } yield (result, optCreds)).tflatMap {
      case (Right(user), _) ⇒ provide(user)
      case (Left(challenge), Some(creds)) ⇒
        AuthRejections.credentialsRejected[StoreAdmin](challenge)
      case (Left(challenge), _) ⇒
        AuthRejections.credentialsMissing[StoreAdmin](challenge)
    }

  def requireCustomerAuth(auth: AsyncAuthenticator[Customer]): AuthenticationDirective[Customer] =
    (for {
      optCreds ← auth.readCredentials()
      result   ← onSuccess(auth.checkAuth(optCreds))
    } yield (result, optCreds)).tflatMap {
      case (Right(user), _) ⇒
        if (!user.isGuest) provide(user)
        else {
          AuthPayload(token = CustomerToken.fromCustomer(user)) match {
            case Xor.Right(authPayload) ⇒
              val header = respondWithHeader(RawHeader("JWT", authPayload.jwt))
              val cookie = setCookie(JwtCookie(authPayload))
              header & cookie & provide(user)
            case Xor.Left(failures) ⇒
              val challenge = FailureChallenge(JwtCustomer.realmName, failures)
              AuthRejections.credentialsRejected[Customer](challenge)
          }
        }
      case (Left(challenge), Some(creds)) ⇒
        AuthRejections.credentialsRejected[Customer](challenge)
      case (Left(challenge), _) ⇒
        AuthRejections.credentialsMissing[Customer](challenge)
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

    def auth[M, F <: EmailFinder[M], T <: Token](finder: F,
                                                 getHashedPassword: M ⇒ Option[String],
                                                 tokenFromModel: M ⇒ T): Result[T] =
      (for {
        userInstance ← * <~ finder(payload.email).mustFindOr(LoginFailed)
        validatedUser ← * <~ validatePassword(userInstance,
                                              getHashedPassword(userInstance),
                                              payload.password)
        _            ← * <~ checkState(payload.kind, userInstance, finder)
        checkedToken ← * <~ tokenFromModel(validatedUser)
      } yield checkedToken).run()

    val tokenResult = payload.kind match {
      case Identity.Admin ⇒
        auth[StoreAdmin, EmailFinder[StoreAdmin], AdminToken](StoreAdmins.findByEmail,
                                                              _.hashedPassword,
                                                              AdminToken.fromAdmin)
      case Identity.Customer ⇒
        auth[Customer, EmailFinder[Customer], CustomerToken](Customers.findByEmail,
                                                             _.hashedPassword,
                                                             CustomerToken.fromCustomer)
    }

    tokenResult.map(_.flatMap { token ⇒
      authTokenLoginResponse(token)
    })
  }

  private def validatePassword[M](model: M,
                                  hashedPassword: Option[String],
                                  password: String): Failures Xor M = {
    val hash = hashedPassword.getOrElse("")
    if (checkPassword(password, hash))
      Xor.right(model)
    else
      Xor.left(LoginFailed.single)
  }

  private def checkState[M](model: M): Failures Xor M = {
    model match {
      case m: StoreAdmin ⇒
        if (m.canLogin) Xor.right(model)
        else Xor.left(AuthFailed(reason = "Store admin is Inactive or Archived").single)
      case _ ⇒
        Xor.right(model)
    }
  }

  private def adminFromToken(token: Token): Failures Xor DBIO[Option[StoreAdmin]] = {
    token match {
      case token: AdminToken ⇒ Xor.right(StoreAdmins.findByIdAndRatchet(token.id, token.ratchet))
      case _                 ⇒ Xor.left(AuthFailed("invalid token").single)
    }
  }

  private def customerFromToken(token: Token): Failures Xor DBIO[Option[Customer]] = {
    token match {
      case token: CustomerToken ⇒ Xor.right(Customers.findByIdAndRatchet(token.id, token.ratchet))
      case _                    ⇒ Xor.left(AuthFailed("invalid token").single)
    }
  }
}
