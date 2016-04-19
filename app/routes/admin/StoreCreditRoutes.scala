package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads._
import services.{StoreCreditAdjustmentsService, StoreCreditService}
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._

object StoreCreditRoutes {
  private[admin] def storeCreditRoutes(implicit ec: EC, db: DB, ac: AC, admin: StoreAdmin) = {
    pathPrefix("store-credits") {
      (patch & pathEnd & entity(as[StoreCreditBulkUpdateStateByCsr])) { payload ⇒
        goodOrFailures {
          StoreCreditService.bulkUpdateStateByCsr(payload, admin)
        }
      }
    } ~
    pathPrefix("store-credits" / IntNumber) { storeCreditId ⇒
      (get & pathEnd) {
        goodOrFailures {
          StoreCreditService.getById(storeCreditId)
        }
      } ~
      (patch & pathEnd & entity(as[StoreCreditUpdateStateByCsr])) { payload ⇒
        goodOrFailures {
          StoreCreditService.updateStateByCsr(storeCreditId, payload, admin)
        }
      } ~
      (get & path("transactions") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
        goodOrFailures {
          StoreCreditAdjustmentsService.forStoreCredit(storeCreditId)
        }
      }
    }
  }
}