import models._

import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{Suite, SuiteMixin}
import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import server.Service
import util.DbTestSupport
import scala.concurrent.Future

import utils.Apis

trait AutomaticAuth extends SuiteMixin
  with ScalaFutures
  with HttpSupport { this: Suite with PatienceConfiguration with DbTestSupport ⇒

  val authedStoreAdmin = StoreAdmin(id = 1, email = "donkey@donkey.com", password = "donkeyPass",
          firstName = "Mister", lastName = "Donkey")

  val authedCustomer = Customer(id = 1, email = "donkey@donkey.com", password = "donkeyPass",
          firstName = "Mister", lastName = "Donkey")

  def makeApis: Option[Apis] = None

  override def makeService: Service = {
    new Service(dbOverride = Some(db), systemOverride = Some(system), apisOverride = makeApis) {
      override def storeAdminAuth: AsyncAuthenticator[StoreAdmin] = (UserCredentials) => {
        Future.successful(Some(authedStoreAdmin))
      }

      override def customerAuth: AsyncAuthenticator[Customer] = (UserCredentials) => {
        Future.successful(Some(authedCustomer))
      }
    }
  }
}
