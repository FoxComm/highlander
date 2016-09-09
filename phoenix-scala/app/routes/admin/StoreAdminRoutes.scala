package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.auth.AdminToken
import payloads.StoreAdminPayloads._
import services.StoreAdminManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object StoreAdminRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("store-admins") {
        (post & pathEnd & entity(as[CreateStoreAdminPayload])) { payload ⇒
          mutateOrFailures {
            StoreAdminManager.create(payload, admin)
          }
        } ~
        pathPrefix(IntNumber) { saId ⇒
          (get & pathEnd) {
            getOrFailures {
              StoreAdminManager.getById(saId)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateStoreAdminPayload])) { payload ⇒
            mutateOrFailures {
              StoreAdminManager.update(saId, payload, admin)
            }
          } ~
          (delete & pathEnd) {
            deleteOrFailures {
              StoreAdminManager.delete(saId, admin)
            }
          } ~
          pathPrefix("state") {
            (patch & pathEnd & entity(as[StateChangeStoreAdminPayload])) { payload ⇒
              mutateOrFailures {
                StoreAdminManager.changeState(saId, payload, admin)
              }
            }
          }
        } ~
        pathPrefix("me") {
          (get & pathEnd) {
            complete(AdminToken.fromAdmin(admin))
          }
        }
      }
    }
  }
}
