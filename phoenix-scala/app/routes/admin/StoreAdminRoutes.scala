package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.StoreAdminPayloads._
import services.StoreAdminManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object StoreAdminRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("store-admins") {
        (post & pathEnd & entity(as[CreateStoreAdminPayload])) { payload ⇒
          mutateOrFailures {
            StoreAdminManager.create(payload, Some(auth.model))
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
              StoreAdminManager.update(saId, payload, auth.model)
            }
          } ~
          (delete & pathEnd) {
            deleteOrFailures {
              StoreAdminManager.delete(saId, auth.model)
            }
          } ~
          pathPrefix("state") {
            (patch & pathEnd & entity(as[StateChangeStoreAdminPayload])) { payload ⇒
              mutateOrFailures {
                StoreAdminManager.changeState(saId, payload, auth.model)
              }
            }
          }
        }
      }
    }
  }
}
