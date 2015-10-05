package routes.admin

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models._
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.SprayDirectives._

object Admin {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      pathPrefix("store-credits") {
        (patch & entity(as[payloads.StoreCreditBulkUpdateStatusByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            StoreCreditService.bulkUpdateStatusByCsr(payload, admin)
          }
        }
      } ~
      pathPrefix("store-credits" / IntNumber) { storeCreditId ⇒
        (get & pathEnd) {
          goodOrFailures {
            StoreCreditService.getById(storeCreditId)
          }
        } ~
        (patch & entity(as[payloads.StoreCreditUpdateStatusByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            StoreCreditService.updateStatusByCsr(storeCreditId, payload, admin)
          }
        } ~
        (get & path("transactions") & pathEnd) {
          goodOrFailures {
            StoreCreditAdjustmentsService.forStoreCredit(storeCreditId)
          }
        }
      } ~
      pathPrefix("reasons") {
        (get & pathEnd) {
          good {
            ReasonService.listAll
          }
        }
      } ~
      pathPrefix("shipping-methods" / OrderRoutes.orderRefNum) { refNum ⇒
        (get & pathEnd) {
          goodOrFailures {
            Orders.findByRefNum(refNum).findOneAndRunIgnoringLock { order ⇒
              ShippingManager.getShippingMethodsForOrder(order)
            }
          }
        }
      } ~
      pathPrefix("notifications") {
        (get & pathEnd) {
          good {
            Seq(
              Notification("Delivered", "Shipment Confirmation", "2015-02-15T08:31:45", "jim@bob.com"),
              Notification("Failed", "Order Confirmation", "2015-02-16T09:23:29", "+ (567) 203-8430")
            )
          }
        } ~
        (get & path(IntNumber) & pathEnd) { notificationId ⇒
          good {
            Notification("Failed", "Order Confirmation", "2015-02-16T09:23:29", "+ (567) 203-8430")
          }
        }
      }
    }
  }
}

