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

  type FutureAuthResult[M] = Future[AuthenticationResult[M]]
  type AsyncAuthenticator[M] = (Option[HttpCredentials]) ⇒ FutureAuthResult[M]

  final case class Credentials(identifier: String, secret: String)

  def customer(credentials: Option[HttpCredentials])(implicit ec: EC, db: DB): Future[AuthenticationResult[Customer]] = {
    auth[Customer, EmailFinder[Customer]]("private customer routes")(credentials, Customers.findByEmail, _.hashedPassword)
  }

  def storeAdmin(credentials: Option[HttpCredentials])
              (implicit ec: EC, db: DB): Future[AuthenticationResult[StoreAdmin]] = {
    auth[StoreAdmin, EmailFinder[StoreAdmin]]("admin")(credentials, StoreAdmins.findByEmail, _.hashedPassword)
  }

  def requireAdminAuth(auth: AsyncAuthenticator[StoreAdmin]): AuthenticationDirective[StoreAdmin] = {
    extractExecutionContext.flatMap { implicit ec ⇒
      authenticateOrRejectWithChallenge(auth)
    }
  }

  def requireCustomerAuth(auth: AsyncAuthenticator[Customer]): AuthenticationDirective[Customer] = {
    extractExecutionContext.flatMap { implicit ec ⇒
      authenticateOrRejectWithChallenge(auth)
    }
  }

  private[this] def auth[M, F <: EmailFinder[M]](realm: String)
    (credentials: Option[HttpCredentials], finder: F, getHashedPassword: M ⇒ Option[String])
   (implicit ec: EC, db: DB): Future[AuthenticationResult[M]] = {
    credentials.flatMap(extractCredentials) match {
      case Some(p) ⇒
        finder(p.identifier).run().map { optModel ⇒
          optModel.filter { userModel ⇒
            getHashedPassword(userModel).exists(checkPassword(p.secret, _))
          }
        }.map {
          case Some(instance) ⇒ AuthenticationResult.success(instance)
          case None ⇒ AuthenticationResult.failWithChallenge(challengeFor(realm))
        }
      case _ ⇒
        Future.successful(AuthenticationResult.failWithChallenge(challengeFor(realm)))
    }
  }

  private def extractCredentials(cred: HttpCredentials): Option[Credentials] = cred match {
    case BasicHttpCredentials(username, secret) ⇒ Some(Credentials(username, secret))
    case OAuth2BearerToken(token) ⇒ Some(Credentials(token, token))
    case _ ⇒ None
  }

  def adminLogin(payload: LoginPayload)
    (implicit ec: ExecutionContext, db: Database): Result[AdminToken] = {
    (for {
      storeAdmin ← * <~ StoreAdmins
        .findByEmail(payload.email)
        .mustFindOr(LoginFailed)
      checked ← * <~ checkAndBuild(storeAdmin, payload.password)
    } yield checked).run()
  }

  private def checkAndBuild(admin: StoreAdmin, password: String): Failures Xor AdminToken = {
    val hash = admin.hashedPassword.getOrElse("")
    if (checkPassword(password, hash))
      Xor.right(AdminToken.fromAdmin(admin))
    else
      Xor.left(LoginFailed.single)
  }

}
