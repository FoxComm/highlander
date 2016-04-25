package routes.admin

import akka.http.scaladsl.server.Directives._
import models.StoreAdmin
import services.InventoryManager
import utils.http.CustomDirectives._
import utils.aliases._

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
          }
        }
      }
    }
  }
}
