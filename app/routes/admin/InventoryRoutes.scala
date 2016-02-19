package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import models.StoreAdmin
import services.InventoryManager
import services.Authenticator.{AsyncAuthenticator, requireAdmin}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._

object InventoryRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    requireAdmin(storeAdminAuth) { admin ⇒
      activityContext(admin) { implicit ac ⇒

        pathPrefix("inventory") {
          pathPrefix("skus" / Segment) { skuCode ⇒
            (get & path(IntNumber) & pathEnd) { warehouseId ⇒
              goodOrFailures {
                InventoryManager.getSkuDetails(skuCode, warehouseId)
              }
            } ~
            (get & pathPrefix("summary") & pathEnd) {
              goodOrFailures {
                InventoryManager.getSkuSummary(skuCode)
              }
            }
          }
        }
      }
    }
  }
}
