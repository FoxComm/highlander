package services

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.{AuthenticationDirective, AuthenticationResult}
import akka.http.scaladsl.server.Directives.extractExecutionContext
import akka.http.scaladsl.server.directives.SecurityDirectives.{AuthenticationResult, authenticateOrRejectWithChallenge, challengeFor}
import akka.http.scaladsl.model.headers.HttpCredentials
import models.customer.{Customer, Customers}
import cats.data.Xor
import models.{StoreAdmin, StoreAdmins}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Passwords.checkPassword
import utils.aliases._
import models.auth.Identity
import models.auth.{AdminToken, CustomerToken, Token}
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

  final case class Credentials(identifier: String, secret: String)

  def customer(credentials: Option[HttpCredentials])
    (implicit ec: EC, db: DB): Future[AuthenticationResult[Customer]] = {
    auth[Customer, EmailFinder[Customer]]("private customer routes")(credentials, Customers.findByEmail, _.hashedPassword)
  }

  def storeAdmin(credentials: Option[HttpCredentials])
    (implicit ec: EC, db: DB): Future[AuthenticationResult[StoreAdmin]] = {
    auth[StoreAdmin, EmailFinder[StoreAdmin]]("admin")(credentials, StoreAdmins.findByEmail, _.hashedPassword)
  }

  def requireAuth[M](auth: AsyncAuthenticator[M]): AuthenticationDirective[M] = {
    extractExecutionContext.flatMap { implicit ec ⇒
      authenticateOrRejectWithChallenge(auth)
    }
  }

  private[this] def auth[M, F <: EmailFinder[M]](realm: String)
    (credentials: Option[HttpCredentials], finder: F, getHashedPassword: M ⇒ Option[String])
   (implicit ec: EC, db: DB): Future[AuthenticationResult[M]] = {
    (for {
      userCredentials ← * <~ credentials.flatMap(extractCredentials).toXor(LoginFailed.single)
      user ← * <~ finder(userCredentials.identifier).mustFindOr(LoginFailed)
      validated ← * <~ validatePassword(user, getHashedPassword(user), userCredentials.secret)
    } yield validated).runTxn().map {
      case Xor.Right(entity) ⇒ AuthenticationResult.success(entity)
      case Xor.Left(_) ⇒ AuthenticationResult.failWithChallenge(challengeFor(realm))
    }
  }

  private def extractCredentials(cred: HttpCredentials): Option[Credentials] = cred match {
    case BasicHttpCredentials(username, secret) ⇒ Some(Credentials(username, secret))
    case OAuth2BearerToken(token) ⇒ Some(Credentials(token, token))
    case _ ⇒ None
  }

  def authenticate(payload: LoginPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Token] = {

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
