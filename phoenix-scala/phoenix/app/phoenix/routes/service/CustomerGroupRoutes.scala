package routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.CustomerGroupPayloads.CustomerGroupMemberServiceSyncPayload
import services.Authenticator.AuthData
import services.customerGroups.{GroupManager, GroupMemberManager}
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives.{activityContext, _}
import utils.http.Http._

object CustomerGroupRoutes {

  def routes(implicit ec: EC, db: DB, apis: Apis, auth: AuthData[User]): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("customer-groups") {
        (get & pathEnd) {
          getOrFailures {
            GroupManager.findAll
          }
        }
      } ~
      pathPrefix("customer-groups" / IntNumber) { groupId ⇒
        pathPrefix("customers") {
          (post & pathEnd & entity(as[CustomerGroupMemberServiceSyncPayload])) { payload ⇒
            doOrFailures(
                GroupMemberManager.sync(groupId, payload)
            )
          }
        }
      }
    }
  }
}
