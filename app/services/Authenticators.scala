package services

import scala.concurrent.Future
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, OAuth2BearerToken, BasicHttpCredentials,
HttpCredentials}
import akka.http.scaladsl.server.directives.{AuthenticationResult, AuthenticationDirective}
import akka.http.scaladsl.server.Directives.extractExecutionContext
import akka.http.scaladsl.server.directives.SecurityDirectives.{AuthenticationResult, challengeFor,
  authenticateOrRejectWithChallenge}
import models.customer.{Customer, Customers}
import cats.data.Xor
import models.{StoreAdmin, StoreAdmins}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Passwords.checkPassword
import utils.aliases._
import models.auth.Identity
import models.auth._
import payloads.LoginPayload
import utils.DbResultT._
import utils.DbResultT.implicits._

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication

object Authenticator {

  type EmailFinder[M] = String ⇒ DBIO[Option[M]]
  type AsyncAuthenticator[M] = (Option[HttpCredentials]) ⇒ Future[AuthenticationResult[M]]

  def customer(credentials: Option[HttpCredentials])
              (implicit ec: EC, db: DB): Future[AuthenticationResult[Customer]] = {
    authCheck[Customer, EmailFinder[Customer]]("private customer routes")(credentials, Customers.findByEmail, _.hashedPassword)
  }

  def storeAdmin(credentials: Option[HttpCredentials])
              (implicit ec: EC, db: DB): Future[AuthenticationResult[StoreAdmin]] = {
    authCheck[StoreAdmin, EmailFinder[StoreAdmin]]("admin")(credentials, StoreAdmins.findByEmail, _.hashedPassword)
  }

  def requireAuth[M](auth: AsyncAuthenticator[M]): AuthenticationDirective[M] = {
    extractExecutionContext.flatMap { implicit ec ⇒
      authenticateOrRejectWithChallenge(auth)
    }
  }

  private[this] def authCheck[M, F <: EmailFinder[M]](realm: String)
    (credentials: Option[HttpCredentials], finder: F, getHashedPassword: M ⇒ Option[String])
   (implicit ec: EC, db: DB): Future[AuthenticationResult[M]] = {
    (for {
      userCredentials ← * <~ credentials.flatMap {
        case BasicHttpCredentials(username, secret) ⇒ Some(SecretCredentials(username, secret))
        case _ ⇒ None
      }.toXor(LoginFailed.single)
      user ← * <~ finder(userCredentials.identifier).mustFindOr(LoginFailed)
      validated ← * <~ validatePassword(user, getHashedPassword(user), userCredentials.secret)
    } yield validated).runTxn().map {
      case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
      case Xor.Left(_) ⇒ AuthenticationResult.failWithChallenge(challengeFor(realm))
    }
  }

  private def extractCredentials(cred: HttpCredentials): Option[Credentials] = cred match {
    case BasicHttpCredentials(username, secret) ⇒ Some(SecretCredentials(username, secret))
    case OAuth2BearerToken(token) ⇒ Some(OauthCredentials(token))
    // assume it's JWT, also it's not a typo - token contained in scheme for GenericHttpCredentials
    case GenericHttpCredentials(scheme, token, params) ⇒ Some(JWTCredentials(scheme))
    case _ ⇒ None
  }

  def authenticate(payload: LoginPayload)
    (implicit ec: EC, db: DB): Result[Token] = {

    def auth[M, F <: EmailFinder[M], T](finder: F, getHashedPassword: M ⇒ Option[String], builder: M ⇒ T): Result[T] = {
      (for {
        instance ← * <~ finder(payload.email).mustFindOr(LoginFailed)
        validated ← * <~ validatePassword(instance, getHashedPassword(instance), payload.password)
        checked ← * <~ builder(validated)
      } yield checked).run()
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

}
