package routes.admin

import akka.http.scaladsl.server.Directives._
import models.StoreAdmin
import models.inventory.InventoryAdjustment.WmsOverride
import services.InventoryManager
import services.inventory.InventoryAdjustmentManager
import utils.http.CustomDirectives._
import utils.aliases._
import utils.http.Http._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

object InventoryRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      determineObjectContext(db, ec) { productContext ⇒

        pathPrefix("inventory") {
          pathPrefix("skus" / Segment) { skuCode ⇒
            (get & path(IntNumber) & pathEnd) { warehouseId ⇒
              goodOrFailures {
                InventoryManager.getSkuDetails(skuCode, warehouseId, productContext)
              }
            } ~
            (get & pathPrefix("summary") & pathEnd) {
              goodOrFailures {
                InventoryManager.getSkuSummary(skuCode, productContext)
              }
            }
          } ~
          pathPrefix("wms" / "override") {
            (post & pathEnd & entity(as[payloads.WmsEventPayload])) { event ⇒
              nothingOrFailures {
                 InventoryAdjustmentManager.wmsOverride(event)
              }
            }
          }
        }
      }
    }
  }
}
