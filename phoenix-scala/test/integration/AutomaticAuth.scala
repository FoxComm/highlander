import scala.concurrent.Future
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.AuthenticationResult
import akka.http.scaladsl.server.directives.BasicDirectives.provide
import akka.http.scaladsl.server.directives.SecurityDirectives._

import models.StoreAdmin
import models.customer.Customer
import org.scalatest.SuiteMixin
import org.scalatest.concurrent.ScalaFutures
import services.Authenticator.AsyncAuthenticator
import testutils.{FoxSuite, HttpSupport}
import utils.seeds.Seeds.Factories

trait FakeAuth[M] extends AsyncAuthenticator[M] {
  type C = String
  def readCredentials(): Directive1[Option[String]] = provide(Some("ok"))
}

case class AuthAs[M](m: M) extends FakeAuth[M] {
  def checkAuth(creds: Option[String]): Future[AuthenticationResult[M]] = {
    Future.successful(AuthenticationResult.success(m))
  }
}

case class AuthFailWith[M](challenge: HttpChallenge) extends FakeAuth[M] {
  def checkAuth(creds: Option[String]): Future[AuthenticationResult[M]] = {
    Future.successful(AuthenticationResult.failWithChallenge(challenge))
  }
}

trait AutomaticAuth extends SuiteMixin with ScalaFutures with HttpSupport { self: FoxSuite ⇒

  val authedStoreAdmin = Factories.storeAdmin.copy(id = 1)

  val authedCustomer = Factories.customer.copy(id = 1)

  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] =
    AuthAs(authedStoreAdmin)

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] =
    AuthAs(authedCustomer)
}
