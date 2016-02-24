package services

import akka.http.scaladsl.model.headers.{OAuth2BearerToken, BasicHttpCredentials}
import akka.http.scaladsl.server.directives.{AuthenticationResult, AuthenticationDirective}
import akka.http.scaladsl.server.Directives.extractExecutionContext
import akka.http.scaladsl.server.directives.SecurityDirectives.{AuthenticationResult, challengeFor,
  authenticateOrRejectWithChallenge}
import akka.http.scaladsl.model.headers.HttpCredentials
import models.customer.{Customers, Customer}
import models.{StoreAdmin, StoreAdmins}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Passwords.checkPassword

import scala.concurrent.{ExecutionContext, Future}

// TODO: Implement real session-based authentication with JWT
// TODO: Probably abstract this out so that we use one for both AdminUsers and Customers
// TODO: Add Roles and Permissions.  Check those before taking on an action
// TODO: Investigate 2-factor Authentication
object Authenticator {

  type EmailFinder[M] = String ⇒ DBIO[Option[M]]

  type FutureAuthResult[M] = Future[AuthenticationResult[M]]
  type AsyncAuthenticator[M] = (Option[HttpCredentials]) ⇒ FutureAuthResult[M]

  final case class Credentials(identifier: String, secret: String)

  def customer(credentials: Option[HttpCredentials])
              (implicit ec: ExecutionContext, db: Database): Future[AuthenticationResult[Customer]] = {
    auth[Customer, EmailFinder[Customer]]("private customer routes")(credentials, Customers.findByEmail, _.hashedPassword)
  }

  def storeAdmin(credentials: Option[HttpCredentials])
              (implicit ec: ExecutionContext, db: Database): Future[AuthenticationResult[StoreAdmin]] = {
    auth[StoreAdmin, EmailFinder[StoreAdmin]]("admin")(credentials, StoreAdmins.findByEmail, m ⇒ Some(m.hashedPassword))
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
   (implicit ec: ExecutionContext, db: Database): Future[AuthenticationResult[M]] = {
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
}
