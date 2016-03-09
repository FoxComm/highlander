package services

import scala.concurrent.Future
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.model.headers.{HttpCookie, HttpChallenge, HttpCredentials, RawHeader}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.CookieDirectives.setCookie
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.RespondWithDirectives.respondWithHeader
import akka.http.scaladsl.server.directives.SecurityDirectives.{AuthenticationResult, challengeFor, extractCredentials}
import akka.http.scaladsl.server.directives.{AuthenticationDirective, AuthenticationResult}

import cats.data.Xor
import models.auth.{Identity, _}
import models.customer.{Customer, Customers}
import models.{StoreAdmin, StoreAdmins}
import payloads.LoginPayload
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Passwords.checkPassword
import utils.Slick.implicits._
import utils.aliases._
import utils.Config.config
import utils.Config.RichConfig

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication

object Authenticator {

  type EmailFinder[M] = String ⇒ DBIO[Option[M]]
  type TokenToModel[M] = Token ⇒ Failures Xor DBIO[Option[M]]
  type AsyncAuthenticator[M] = (Option[HttpCredentials]) ⇒ Future[AuthenticationResult[M]]

  def forAdminFromConfig(implicit ec: EC, db: DB): AsyncAuthenticator[StoreAdmin] = {
    config.getString("auth.method") match {
      case "basic" ⇒ basicStoreAdmin
      case "jwt" ⇒ jwtStoreAdmin
      case method ⇒ throw new RuntimeException(s"unknown auth method $method")
    }
  }

  def forCustomerFromConfig(implicit ec: EC, db: DB): AsyncAuthenticator[Customer] = {
    config.getString("auth.method") match {
      case "basic" ⇒ basicCustomer
      case "jwt" ⇒ jwtCustomer
      case method ⇒ throw new RuntimeException(s"unknown auth method $method")
    }
  }

  def basicCustomer(credentials: Option[HttpCredentials])
              (implicit ec: EC, db: DB): Future[AuthenticationResult[Customer]] = {
    basicAuth[Customer, EmailFinder[Customer]]("private customer routes")(credentials, Customers.findByEmail, _.hashedPassword)
  }

  def basicStoreAdmin(credentials: Option[HttpCredentials])
              (implicit ec: EC, db: DB): Future[AuthenticationResult[StoreAdmin]] = {
    basicAuth[StoreAdmin, EmailFinder[StoreAdmin]]("admin")(credentials, StoreAdmins.findByEmail, _.hashedPassword)
  }

  def requireAuth[T](auth: AsyncAuthenticator[T]): AuthenticationDirective[T] = {
    extractCredentials.flatMap { optCreds ⇒
      onSuccess(auth(optCreds)).flatMap {
        case Right(user) ⇒
          provide(user)
        case Left(challenge) ⇒
          val cause = if (optCreds.isEmpty) CredentialsMissing else CredentialsRejected
          reject(AuthenticationFailedRejection(cause, challenge)): Directive1[T]
      }
    }
  }

  def responseWithToken(token: Token): Route = {
    val claims = Token.getJWTClaims(token)
    val siteClaims = Token.getJWTClaims(token)
    claims.setSubject("API")
    siteClaims.setSubject("site")

    respondWithHeader(RawHeader("JWT", Token.encodeJWTClaims(claims))).& (
      setCookie(HttpCookie(
        name = "JWT",
        value = Token.encodeJWTClaims(siteClaims),
        secure = config.getOptBool("auth.cookieSecure").getOrElse(true),
        httpOnly = true,
        expires = config.getOptLong("auth.cookieTTL").map { ttl ⇒
          DateTime.now  + ttl*1000
        },
        path = Some("/"),
        domain = config.getOptString("auth.cookieDomain")
      ))) {
        complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, claims.toJson)))
    }
  }

  def jwtCustomer(credentials: Option[HttpCredentials])
    (implicit ec: EC, db: Database): Future[AuthenticationResult[Customer]] = {
    jwtAuth[Customer, TokenToModel[Customer]]("private customer routes")(credentials, customerFromToken)
  }

  def jwtStoreAdmin(credentials: Option[HttpCredentials])
    (implicit ec: EC, db: Database): Future[AuthenticationResult[StoreAdmin]] = {
    jwtAuth[StoreAdmin, TokenToModel[StoreAdmin]]("admin")(credentials, adminFromToken)
  }

  private[this] def jwtAuth[M, F <: TokenToModel[M]](realm: String)
    (credentials: Option[HttpCredentials], userFromToken: F)
    (implicit ec: EC, db: Database): Future[AuthenticationResult[M]] = (for {
      jwtCredentials ← * <~ Credentials.mustVerifyJWTCredentials(credentials, LoginFailed.single)
      token          ← * <~ validateTokenCredentials(jwtCredentials)
      userDbio       ← * <~ userFromToken(token)
      user           ← * <~ userDbio.mustFindOr(LoginFailed)
    } yield user).run().map {
      case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
      case Xor.Left(_) ⇒ AuthenticationResult.failWithChallenge(HttpChallenge(scheme = "Bearer",
        realm = realm,
        params = Map.empty))
    }

  private[this] def basicAuth[M, F <: EmailFinder[M]](realm: String)
    (credentials: Option[HttpCredentials], finder: F, getHashedPassword: M ⇒ Option[String])
   (implicit ec: EC, db: DB): Future[AuthenticationResult[M]] = {
    (for {
      userCredentials ← * <~ Credentials.mustVerifyBasicCredentials(credentials, LoginFailed.single)
      user            ← * <~ finder(userCredentials.identifier).mustFindOr(LoginFailed)
      validated       ← * <~ validatePassword(user, getHashedPassword(user), userCredentials.secret)
    } yield validated).run().map {
      case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
      case Xor.Left(_) ⇒ AuthenticationResult.failWithChallenge(challengeFor(realm))
    }
  }

  def authenticate(payload: LoginPayload)
    (implicit ec: EC, db: DB): Result[Token] = {

    def auth[M, F <: EmailFinder[M], T <: Token](finder: F, getHashedPassword: M ⇒ Option[String], tokenFromModel: M ⇒
      T):
      Result[T] = {
      (for {
        userInstance  ← * <~ finder(payload.email).mustFindOr(LoginFailed)
        validatedUser ← * <~ validatePassword(userInstance, getHashedPassword(userInstance), payload.password)
        checkedToken  ← * <~ tokenFromModel(validatedUser)
      } yield checkedToken).run()
    }

    payload.kind match {
      case Identity.Admin ⇒
        auth[StoreAdmin, EmailFinder[StoreAdmin], AdminToken](StoreAdmins.findByEmail,
          _.hashedPassword, AdminToken.fromAdmin)
      case Identity.Customer ⇒
        auth[Customer, EmailFinder[Customer], CustomerToken](Customers.findByEmail,
          _.hashedPassword, CustomerToken.fromCustomer)
    }
  }

  private def validatePassword[M](model: M, hashedPassword: Option[String], password: String): Failures Xor M = {
    val hash = hashedPassword.getOrElse("")
    if (checkPassword(password, hash))
      Xor.right(model)
    else
      Xor.left(LoginFailed.single)
  }

  private def validateTokenCredentials(token: JWTCredentials): Failures Xor Token = {
    Token.fromString(token.secret)
  }

  private def adminFromToken(token: Token): Failures Xor DBIO[Option[StoreAdmin]] = {
    token match {
      case token: AdminToken ⇒ Xor.right(DBIO.successful(Some(StoreAdmin(id = token.id,
        email = token.email,
        hashedPassword = None,
        name = token.name.getOrElse(""),
        department = token.department))))
      case _ ⇒ Xor.left(LoginFailed.single)
    }
  }

  private def customerFromToken(token: Token): Failures Xor DBIO[Option[Customer]] = {
    token match {
      case token: CustomerToken ⇒ Xor.right(Customers.findOneById(token.id))
      case _ ⇒ Xor.left(LoginFailed.single)
    }
  }

}
