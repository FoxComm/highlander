package routes.admin

import java.time.Instant

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import models._
import payloads._
import responses.FullOrder.DisplayLineItem
import responses.CustomerResponse.{Root ⇒ Customer}
import responses.StoreAdminResponse
import responses.RmaResponse._

import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

object RmaRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      pathPrefix("rmas") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          good {
            buildMockSequence(Some(StoreAdminResponse.build(admin)))
          }
        } ~
        (get & path("customer" / IntNumber)) { customerId ⇒
          (pathEnd & sortAndPage) { implicit sortAndPage ⇒
            good {
              val customer = Customer(
                id = customerId,
                email = "donkey@donkeyville.com",
                disabled = false,
                blacklisted = false,
                rank = "Donkey",
                createdAt = Instant.now())

              buildMockSequence(admin = None, customer = Some(customer))
            }
          }
        } ~
        (get & path("order" / Order.orderRefNumRegex)) { refNum ⇒
          (pathEnd & sortAndPage) { implicit sortAndPage ⇒
            good {
              buildMockSequence(Some(StoreAdminResponse.build(admin)))
            }
          }
        }
      } ~
      pathPrefix("rmas" / """([a-zA-Z0-9-_]*)""".r) { refNum ⇒
        (get & pathEnd) {
          good {
            buildMockRma(id = 1, refNum = "ABC-123", orderId = 1, admin = Some(StoreAdminResponse.build(admin)))
          }
        }
      }
    }
  }
}