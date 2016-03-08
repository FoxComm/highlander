import akka.http.scaladsl.server.directives.AuthenticationResult
import models.StoreAdmin
import models.customer.Customer
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{Suite, SuiteMixin}
import util.DbTestSupport
import services.Authenticator.AsyncAuthenticator
import scala.concurrent.Future

trait AutomaticAuth extends SuiteMixin
  with ScalaFutures
  with HttpSupport { this: Suite with PatienceConfiguration with DbTestSupport ⇒

  val authedStoreAdmin = StoreAdmin.build(id = 1, email = "donkey@donkey.com", password = Some("donkeyPass"),
          name = "Mister Donkey")

  val authedCustomer = Customer.build(id = 1, email = "donkey@donkey.com", password = Some("donkeyPass"),
          name = Some("Mister Donkey"))


  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = (UserCredentials) ⇒ {
    Future.successful(AuthenticationResult.success(authedStoreAdmin))
  }

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] = (UserCredentials) ⇒ {
    Future.successful(AuthenticationResult.success(authedCustomer))
  }

}
