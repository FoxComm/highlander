import scala.concurrent.Future

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.AuthenticationResult
import akka.http.scaladsl.server.directives.BasicDirectives.provide
import akka.http.scaladsl.server.directives.SecurityDirectives._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{Suite, SuiteMixin}

import models.account._
import models.auth.UserToken
import services.Authenticator.{UserAuthenticator, AuthData}
import services.account.AccountCreateContext
import util.DbTestSupport
import utils.seeds.Seeds.Factories

abstract class FakeAuth extends UserAuthenticator {
  type C = String
  def readCredentials(): Directive1[Option[String]] = provide(Some("ok"))
}

case class AuthAs(admin: User, customer: User) extends FakeAuth {

  //TODO Provide correct claim map
  def checkAuthUser(creds: Option[String]): Future[AuthenticationResult[AuthData[User]]] = {
    val account = Account(id = admin.accountId)
    val token = UserToken.fromUserAccount(
        admin,
        account,
        Account
          .ClaimSet(scope = "1", roles = List("admin"), claims = Map("frn:filleme" → List("r"))))
    Future.successful(AuthenticationResult.success(AuthData[User](token, admin, account)))
  }

  //TODO Provide correct claim map
  def checkAuthCustomer(creds: Option[String]): Future[AuthenticationResult[AuthData[User]]] = {
    val account = Account(id = customer.accountId)
    val token = UserToken.fromUserAccount(customer,
                                          account,
                                          Account.ClaimSet(scope = "2",
                                                           roles = List("customer"),
                                                           claims = Map("frn:fillme" → List("r"))))
    Future.successful(AuthenticationResult.success(AuthData[User](token, customer, account)))
  }
}

case class AuthFailWith(challenge: HttpChallenge) extends FakeAuth {

  def checkAuthUser(creds: Option[String]): Future[AuthenticationResult[AuthData[User]]] = {
    Future.successful(AuthenticationResult.failWithChallenge(challenge))
  }

  def checkAuthCustomer(creds: Option[String]): Future[AuthenticationResult[AuthData[User]]] = {
    Future.successful(AuthenticationResult.failWithChallenge(challenge))
  }
}

trait AutomaticAuth extends SuiteMixin with ScalaFutures with HttpSupport {
  this: Suite with PatienceConfiguration with DbTestSupport ⇒

  val authedUser     = Factories.storeAdmin.copy(id = 1, accountId = 1)
  val authedCustomer = Factories.customer.copy(id = 2, accountId = 2)

  override def overrideUserAuth: UserAuthenticator =
    AuthAs(authedUser, authedCustomer)
}
