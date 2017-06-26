package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.CustomerGroupPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.customerGroups._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object CustomerGroupsRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("customer-groups") {
        (get & pathEnd) {
          getOrFailures {
            GroupManager.findAll
          }
        } ~
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
