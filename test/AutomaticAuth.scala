import models._

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{Suite, SuiteMixin}
import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import util.DbTestSupport
import scala.concurrent.Future

trait AutomaticAuth extends SuiteMixin
  with ScalaFutures
  with HttpSupport { this: Suite with PatienceConfiguration with DbTestSupport ⇒

  override def makeService: Service = {
    new Service(dbOverride = Some(db), systemOverride = Some(as)) {
      override def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = (UserCredentials) => {
        Future.successful(Some(StoreAdmin(id = 1, email = "donkey@donkey.com", password = "donkeyPass",
          firstName = "Mister", lastName = "Donkey")))
      }

      override def customerAuth: AsyncAuthenticator[Customer] = (UserCredentials) => {
        Future.successful(Some(Customer(id = 1, email = "donkey@donkey.com", password = "donkeyPass",
          firstName = "Mister", lastName = "Donkey")))
      }
    }
  }
}
