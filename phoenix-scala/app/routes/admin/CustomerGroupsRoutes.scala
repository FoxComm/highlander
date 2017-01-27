package routes.admin

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.CustomerGroupPayloads.CustomerDynamicGroupPayload
import services.Authenticator.AuthData
import services.customerGroups.{GroupManager, GroupTemplateManager}
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CustomerGroupsRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]) = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("customer-groups") {
        (post & pathEnd & entity(as[CustomerDynamicGroupPayload])) { payload ⇒
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
        (patch & pathEnd & entity(as[CustomerDynamicGroupPayload])) { payload ⇒
          mutateOrFailures {
            GroupManager.update(groupId, payload, auth.model)
          }
        } ~
        (delete & pathEnd) {
          deleteOrFailures {
            GroupManager.delete(groupId, auth.model)
          }
        }
      }
    }
  }
}
