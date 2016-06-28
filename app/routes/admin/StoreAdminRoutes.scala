package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.auth.AdminToken
import models.traits.Originator
import payloads.StoreAdminPayloads._
import services.StoreAdminManager
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.aliases._
import utils.db.DbResultT.Runners._

object StoreAdminRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("store-admins") {
        (post & pathEnd & entity(as[CreateStoreAdminPayload])) { payload ⇒
          mutateOrFailures(
              StoreAdminManager.create(payload, Originator(admin))
          )
        } ~
        pathPrefix(IntNumber) { saId ⇒
          (get & pathEnd) {
            goodOrFailures(
                StoreAdminManager.getById(saId).run()
            )
          } ~
          (patch & pathEnd & entity(as[UpdateStoreAdminPayload])) { payload ⇒
            mutateOrFailures(
                StoreAdminManager.update(saId, payload, Originator(admin))
            )
          } ~
          (delete & pathEnd) {
            deleteOrFailures(
                StoreAdminManager.delete(saId, Originator(admin))
            )
          }
        }
      } ~
      pathPrefix("admin" / "info") {
        (get & pathEnd) {
          complete(AdminToken.fromAdmin(admin))
        }
      }
    }
  }
}
