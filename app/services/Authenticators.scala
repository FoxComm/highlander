package services

import scala.concurrent.Future
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, HttpChallenge, HttpCookie, HttpCredentials, RawHeader}
import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.CookieDirectives.setCookie
import akka.http.scaladsl.server.directives.RespondWithDirectives.respondWithHeader
import akka.http.scaladsl.server.directives.SecurityDirectives.{AuthenticationResult, challengeFor}
import akka.http.scaladsl.server.directives.{AuthenticationDirective, AuthenticationResult}

import cats.data.Xor
import failures.{AuthFailed, Failures, LoginFailed}
import models.auth.{Identity, _}
import models.customer.{Customer, Customers}
import models.{StoreAdmin, StoreAdmins}
import org.jose4j.jwt.JwtClaims
import payloads.LoginPayload
import slick.driver.PostgresDriver.api._
import utils.Config.{RichConfig, config}
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Passwords.checkPassword
import utils.Slick.implicits._
import utils.aliases._


// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication

final case class AuthPayload(claims: JwtClaims, jwt: String)

object AuthPayload {
  def apply(token: Token): Failures Xor AuthPayload = {
    val claims = Token.getJWTClaims(token)
    Token.encodeJWTClaims(claims).map { encoded ⇒
      AuthPayload(claims = claims, jwt = encoded)
    }
  }
}

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

  private def readCookie() = {
    optionalCookie("JWT").map(_.map { cookie ⇒
      GenericHttpCredentials(scheme = cookie.value, params = Map.empty)
    })
  }

  private def readHeader() = {
    optionalHeaderValueByName("JWT").map(_.map { header ⇒
      GenericHttpCredentials(scheme = header, params = Map.empty)
    })
  }

  private def readCookieOrHeader() = {
    readCookie().flatMap(_.fold(readHeader())(v ⇒ provide(Some(v))))
  }

  def requireAuth[T](auth: AsyncAuthenticator[T]): AuthenticationDirective[T] = {
    readCookieOrHeader().flatMap { optCreds ⇒
      onSuccess(auth(optCreds)).flatMap {
        case Right(user) ⇒
          provide(user)
        case Left(challenge) ⇒
          val cause = if (optCreds.isEmpty) CredentialsMissing else CredentialsRejected
          reject(AuthenticationFailedRejection(cause, challenge)): Directive1[T]
      }
    }
  }


  def authTokenBaseResponse(token: Token, response: AuthPayload ⇒ StandardRoute): Failures Xor Route = {
    for {
      authPayload ← AuthPayload(token)
    } yield respondWithHeader(RawHeader("JWT", authPayload.jwt)).& (
      setCookie(HttpCookie(
        name = "JWT",
        value = authPayload.jwt,
        secure = config.getOptBool("auth.cookieSecure").getOrElse(true),
        httpOnly = true,
        expires = config.getOptLong("auth.cookieTTL").map { ttl ⇒
          DateTime.now + ttl*1000
        },
        path = Some("/"),
        domain = config.getOptString("auth.cookieDomain")
      ))) {
      response(authPayload)
    }
  }
  def authTokenLoginResponse(token: Token): Failures Xor Route = {
    authTokenBaseResponse(token, { payload ⇒
      complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, payload.claims.toJson)))
    })
  }

  def oauthTokenLoginResponse(token: Token): Failures Xor Route = {
    authTokenBaseResponse(token, { payload ⇒
      redirect(Uri./, StatusCodes.Found)
    })
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
      jwtCredentials ← * <~ Credentials.mustVerifyJWTCredentials(credentials, AuthFailed("missed credentials").single)
      token          ← * <~ validateTokenCredentials(jwtCredentials)
      userDbio       ← * <~ userFromToken(token)
      user           ← * <~ userDbio.mustFindOr(LoginFailed)
    } yield user).run().map {
      case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
      case Xor.Left(f) ⇒ AuthenticationResult.failWithChallenge(HttpChallenge(scheme = "Bearer",
        realm = realm,
        params = Map("error" → f.head.description)))
    }

  private[this] def basicAuth[M, F <: EmailFinder[M]](realm: String)
    (credentials: Option[HttpCredentials], finder: F, getHashedPassword: M ⇒ Option[String])
   (implicit ec: EC, db: DB): Future[AuthenticationResult[M]] = {
    (for {
      userCredentials ← * <~ Credentials.mustVerifyBasicCredentials(credentials, AuthFailed("missed credentials").single)
      user            ← * <~ finder(userCredentials.identifier).mustFindOr(LoginFailed)
      validated       ← * <~ validatePassword(user, getHashedPassword(user), userCredentials.secret)
    } yield validated).run().map {
      case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
      case Xor.Left(_) ⇒ AuthenticationResult.failWithChallenge(challengeFor(realm))
    }
  }

  def authenticate(payload: LoginPayload)
    (implicit ec: EC, db: DB): Result[Route] = {

    def auth[M, F <: EmailFinder[M], T <: Token]
      (finder: F, getHashedPassword: M ⇒ Option[String], tokenFromModel: M ⇒ T): Result[T] = (for {
        userInstance  ← * <~ finder(payload.email).mustFindOr(LoginFailed)
        validatedUser ← * <~ validatePassword(userInstance, getHashedPassword(userInstance), payload.password)
        checkedToken  ← * <~ tokenFromModel(validatedUser)
      } yield checkedToken).run()

    val tokenResult = payload.kind match {
      case Identity.Admin ⇒
        auth[StoreAdmin, EmailFinder[StoreAdmin], AdminToken](StoreAdmins.findByEmail,
          _.hashedPassword, AdminToken.fromAdmin)
      case Identity.Customer ⇒
        auth[Customer, EmailFinder[Customer], CustomerToken](Customers.findByEmail,
          _.hashedPassword, CustomerToken.fromCustomer)
    }

    tokenResult.map(_.flatMap { token ⇒
      authTokenLoginResponse(token)
    })
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
      case _ ⇒ Xor.left(AuthFailed("invalid token").single)
    }
  }

  private def customerFromToken(token: Token): Failures Xor DBIO[Option[Customer]] = {
    token match {
      case token: CustomerToken ⇒ Xor.right(Customers.findOneById(token.id))
      case _ ⇒ Xor.left(AuthFailed("invalid token").single)
    }
  }

}
