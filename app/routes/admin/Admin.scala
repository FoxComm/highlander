package routes.admin

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models._
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._

object Admin {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒
      pathPrefix("store-credits") {
        (patch & entity(as[payloads.StoreCreditBulkUpdateStatusByCsr]) & pathEnd) { payload ⇒
          complete {
            StoreCreditService.bulkUpdateStatusByCsr(payload, admin).map(renderGoodOrFailures)
          }
        }
      } ~
      pathPrefix("store-credits" / IntNumber) { storeCreditId ⇒
        (get & pathEnd) {
          complete {
            StoreCreditService.getById(storeCreditId).map(renderGoodOrFailures)
          }
        } ~
        (patch & entity(as[payloads.StoreCreditUpdateStatusByCsr]) & pathEnd) { payload ⇒
          complete {
            StoreCreditService.updateStatusByCsr(storeCreditId, payload, admin).map(renderGoodOrFailures)
          }
        } ~
        (get & path("transactions") & pathEnd) {
          complete {
            StoreCreditAdjustmentsService.forStoreCredit(storeCreditId).map(renderGoodOrFailures)
          }
        }
      } ~
      pathPrefix("reasons") {
        (get & pathEnd) {
          complete {
            ReasonService.listAll.map(render(_))
          }
        }
      } ~
      pathPrefix("shipping-methods" / OrderRoutes.orderRefNum) { refNum ⇒
        (get & pathEnd) {
          complete {
            Orders.findByRefNum(refNum).findOneAndRunIgnoringLock { order ⇒
              ShippingManager.getShippingMethodsForOrder(order)
            }.map(renderGoodOrFailures)
          }
        }
      } ~
      pathPrefix("notifications") {
        (get & pathEnd) {
          complete {
            val notifications = Seq(
              Notification("Delivered", "Shipment Confirmation", "2015-02-15T08:31:45", "jim@bob.com"),
              Notification("Failed", "Order Confirmation", "2015-02-16T09:23:29", "+ (567) 203-8430")
            )
            render(notifications)
          }
        } ~
        (get & path(IntNumber) & pathEnd) { notificationId ⇒
          complete {
            render(Notification("Failed", "Order Confirmation", "2015-02-16T09:23:29", "+ (567) 203-8430"))
          }
        }
      }
    }
  }
}

