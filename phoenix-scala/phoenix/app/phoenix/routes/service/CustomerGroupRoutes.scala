package phoenix.routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import phoenix.models.account.User
import phoenix.payloads.CustomerGroupPayloads.CustomerGroupMemberServiceSyncPayload
import phoenix.services.Authenticator.AuthData
import phoenix.services.customerGroups.{GroupManager, GroupMemberManager}
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives.{activityContext, _}
import phoenix.utils.http.Http._

object CustomerGroupRoutes {

  def routes(implicit ec: EC, db: DB, apis: Apis, auth: AuthData[User]): Route =
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
