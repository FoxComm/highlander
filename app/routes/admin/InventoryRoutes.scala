package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import models.StoreAdmin
import services.InventoryManager
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import utils.Apis
import utils.CustomDirectives._
import utils.aliases._

object InventoryRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    requireAuth(storeAdminAuth) { admin ⇒
      activityContext(admin) { implicit ac ⇒
        determineProductContext(db, ec) { productContext ⇒ 

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
}
