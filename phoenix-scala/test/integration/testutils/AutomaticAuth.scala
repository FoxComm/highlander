package testutils

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.AuthenticationResult
import akka.http.scaladsl.server.directives.BasicDirectives.provide
import akka.http.scaladsl.server.directives.SecurityDirectives.AuthenticationResult
import models.account._
import models.auth.UserToken
import org.scalatest.SuiteMixin
import services.Authenticator
import services.Authenticator.{AuthData, UserAuthenticator}
import services.account.AccountCreateContext
import utils.aliases.{DB, EC}
import utils.db.{*, _}
import utils.seeds.Seeds.Factories

import scala.concurrent.Future

abstract class FakeAuth extends UserAuthenticator {
  type C = String
  def readCredentials(): Directive1[Option[String]] = provide(Some("ok"))
}

case class VariableAuth(var admin: Option[User], var customer: Option[User])(implicit ex: EC,
                                                                             db: DB)
    extends UserAuthenticator {

  private val customerCreateContext = AccountCreateContext(List("customer"), "merchant", 2)
  private val guestAuthenticator    = Authenticator.forUser(customerCreateContext)

  def readCredentials(): Directive1[Option[String]] = {
    provide(customer.map(_ ⇒ "ok"))
  }

  def checkAuthUser(creds: Option[String]): Future[AuthenticationResult[AuthData[User]]] = {
    admin match {
      case Some(a) ⇒
        AuthAs(a, a).checkAuthUser(creds)
      case None ⇒
        val check = guestAuthenticator.checkAuthUser(creds)
        for {
          result ← * <~ check
          user   ← * <~ result.fold(_ ⇒ None, d ⇒ Some(d.model))
        } yield {
          admin = user
          println("!!!!  look " + user.toString)
        }

        check
    }

  }

  def checkAuthCustomer(creds: Option[String]): Future[AuthenticationResult[AuthData[User]]] = {
    customer match {
      case Some(c) ⇒
        AuthAs(c, c).checkAuthCustomer(creds)
      case None ⇒
        val authCustomer = guestAuthenticator.checkAuthCustomer(creds)
        for {
          result ← * <~ authCustomer
          user   ← * <~ result.fold(_ ⇒ None, d ⇒ Some(d.model))
        } yield {
          customer = user
          println("!!!!  look " + user.toString)
        }

        authCustomer
    }

  }
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

trait AutomaticAuth extends SuiteMixin with HttpSupport { self: FoxSuite ⇒

  val authedUser     = Factories.storeAdmin.copy(id = 1, accountId = 1)
  val authedCustomer = Factories.customer.copy(id = 2, accountId = 2)

  override def overrideUserAuth: UserAuthenticator =
    AuthAs(authedUser, authedCustomer)
}
