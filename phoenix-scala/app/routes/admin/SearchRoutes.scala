package routes.admin

import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import services.Authenticator.AuthData
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._
import payloads.SearchPayloads._
import services.search.SearchIndexManager

object SearchRoutes {
  def routes(implicit ec: EC, db: DB, am: Mat, auth: AuthData[User], apis: Apis): Route = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("search") {
        (post & pathEnd & entity(as[SearchIndexPayload])) { payload ⇒
          mutateOrFailures {
            SearchIndexManager.create(payload)
          }
        } ~
        pathPrefix(IntNumber) { id ⇒
          (get & pathEnd) {
            getOrFailures {
              SearchIndexManager.get(id)
            }
          } ~
          (post & pathEnd & entity(as[SearchIndexPayload])) { payload ⇒
            mutateOrFailures {
              SearchIndexManager.update(id, payload)
            }
          }
        }
      }
    }
  }
}
