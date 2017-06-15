package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.StoreAdminPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.StoreAdminManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object StoreAdminRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
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
