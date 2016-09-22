import akka.http.scaladsl.server.directives.AuthenticationResult

import models.account._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{Suite, SuiteMixin}
import util.DbTestSupport
import services.Authenticator.{UserAuthenticator, AuthData}
import services.account.AccountCreateContext
import scala.concurrent.Future
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.BasicDirectives.provide
import akka.http.scaladsl.server.directives.SecurityDirectives._

import utils.seeds.Seeds.Factories

class FakeAuth extends UserAuthenticator {
  type C = String
  def readCredentials(): Directive1[Option[String]] = provide(Some("ok"))
  def checkAuthUser(credentials: Option[String]): Future[AuthenticationResult[AuthData[User]]]
}

case class AuthAs(m: User) extends FakeAuth {
  def checkAuthUser(creds: Option[String]): Future[AuthenticationResult[User]] = {
    Future.successful(AuthenticationResult.success(m))
  }
  def checkAuthCustomer(creds: Option[String]): Future[AuthenticationResult[User]] = {
    Future.successful(AuthenticationResult.success(m))
  }
}

case class AuthFailWith(challenge: HttpChallenge) extends FakeAuth {
  def checkAuthUser(creds: Option[String]): Future[AuthenticationResult[User]] = {
    Future.successful(AuthenticationResult.failWithChallenge(challenge))
  }
  def checkAuthCustomer(creds: Option[String]): Future[AuthenticationResult[User]] = {
    Future.successful(AuthenticationResult.failWithChallenge(challenge))
  }
}

trait AutomaticAuth extends SuiteMixin with ScalaFutures with HttpSupport {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒

  val authedStoreAdmin = Factories.storeAdmin.copy(accountId = 1)

  val authedCustomer = Factories.customer.copy(accountId = 1)

  override def overrideStoreAdminAuth: UserAuthenticator =
    AuthAs(authedStoreAdmin)

  override def overrideCustomerAuth: UserAuthenticator =
    AuthAs(authedCustomer)
}
