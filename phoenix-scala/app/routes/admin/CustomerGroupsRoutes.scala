package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import services.GroupManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CustomerGroupsRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("groups") {
        (get & pathEnd) {
          getOrFailures {
            GroupManager.findAll
          }
        } ~
        (post & pathEnd & entity(as[CustomerDynamicGroupPayload])) { payload ⇒
          mutateOrFailures {
            GroupManager.create(payload, auth.model)
          }
        }
      } ~
      pathPrefix("groups" / IntNumber) { groupId ⇒
        (get & pathEnd) {
          getOrFailures {
            GroupManager.getById(groupId)
          }
        } ~
        (patch & pathEnd & entity(as[CustomerDynamicGroupPayload])) { payload ⇒
          mutateOrFailures {
            GroupManager.update(groupId, payload)
          }
        }
      }
    }
  }
}
