package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.CustomerGroupPayloads._
import services.Authenticator.AuthData
import services.customerGroups._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CustomerGroupsRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("customer-groups") {
        (post & pathEnd & entity(as[CustomerGroupPayload])) { payload ⇒
          mutateOrFailures {
            GroupManager.create(payload, auth.model)
          }
        } ~
        pathPrefix("templates") {
          (get & pathEnd) {
            getOrFailures {
              GroupTemplateManager.getAll()
            }
          }
        }
      } ~
      pathPrefix("customer-groups" / IntNumber) { groupId ⇒
        (get & pathEnd) {
          getOrFailures {
            GroupManager.getById(groupId)
          }
        } ~
        (patch & pathEnd & entity(as[CustomerGroupPayload])) { payload ⇒
          mutateOrFailures {
            GroupManager.update(groupId, payload, auth.model)
          }
        } ~
        (delete & pathEnd) {
          deleteOrFailures {
            GroupManager.delete(groupId, auth.model)
          }
        } ~
        path("customers") {
          (post & pathEnd & entity(as[CustomerGroupMemberSyncPayload])) { payload ⇒
            doOrFailures(
                GroupMemberManager.sync(groupId, payload)
            )
          }
        }
      }
    }
  }
}
