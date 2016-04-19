package routes.admin

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import payloads._
import services.GroupManager
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._


object CustomerGroupsRoutes {
  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("groups") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            GroupManager.findAll
          }
        } ~
        (post & pathEnd & entity(as[CustomerDynamicGroupPayload])) { payload ⇒
          goodOrFailures {
            GroupManager.create(payload, admin)
          }
        }
      } ~
      pathPrefix("groups" / IntNumber) { groupId ⇒
        (get & pathEnd) {
          goodOrFailures {
            GroupManager.getById(groupId)
          }
        } ~
        (patch & pathEnd & entity(as[CustomerDynamicGroupPayload])) { payload ⇒
          goodOrFailures {
            GroupManager.update(groupId, payload)
          }
        }
      }
    }
  }
}
