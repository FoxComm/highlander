package routes.service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import payloads.CustomerGroupPayloads.CustomerGroupMemberSyncPayload
import services.customerGroups.GroupMemberManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CustomerGroupRoutes {

  def routes()(implicit ec: EC, db: DB, es: ES): Route = {

    activityContext() { implicit ac ⇒
      pathPrefix("customerGroups" / IntNumber) { groupId =>
        pathPrefix("users") {
          (post & pathEnd & entity(as[CustomerGroupMemberSyncPayload])) { data =>
            doOrFailures(
              GroupMemberManager.sync()
            )
          }
        }
      }
    }
  }

}
