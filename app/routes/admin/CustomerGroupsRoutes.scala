package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import payloads._
import services.GroupManager
import services.Authenticator.{AsyncAuthenticator}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._
import utils.Slick.implicits._

import models.auth.Session.requireAdminAuth


object CustomerGroupsRoutes {
  def routes(implicit ec: EC, db: DB, mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    requireAdminAuth { admin ⇒
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
}
