
import models.{Customer, StoreAdmin}
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{Suite, SuiteMixin}
import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import util.DbTestSupport
import scala.concurrent.Future

trait AutomaticAuth extends SuiteMixin
  with ScalaFutures
  with HttpSupport { this: Suite with PatienceConfiguration with DbTestSupport ⇒

  val authedStoreAdmin = StoreAdmin(id = 1, email = "donkey@donkey.com", password = "donkeyPass",
          name = "Mister Donkey")

  val authedCustomer = Customer(id = 1, email = "donkey@donkey.com", password = Some("donkeyPass"),
          name = Some("Mister Donkey"))


  override def overrideStoreAdminAuth: AsyncAuthenticator[StoreAdmin] = (UserCredentials) ⇒ {
    Future.successful(Some(authedStoreAdmin))
  }

  override def overrideCustomerAuth: AsyncAuthenticator[Customer] = (UserCredentials) ⇒ {
    Future.successful(Some(authedCustomer))
  }

}
