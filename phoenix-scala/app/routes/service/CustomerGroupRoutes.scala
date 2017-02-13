package routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import payloads.CustomerGroupPayloads.CustomerGroupMemberServiceSyncPayload
import services.customerGroups.{GroupManager, GroupMemberManager}
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CustomerGroupRoutes {

  def routes(implicit ec: EC, db: DB, es: ES): Route = {
    activityContext() { implicit ac ⇒
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
