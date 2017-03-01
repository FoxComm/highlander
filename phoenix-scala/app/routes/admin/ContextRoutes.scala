package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.ContextPayloads._
import services.Authenticator.AuthData
import services.context.ContextManager
import utils.aliases._
import services.objects.ObjectSchemasManager
import utils.http.CustomDirectives._
import utils.http.Http._

object ContextRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("contexts") {
        (post & pathEnd & entity(as[CreateObjectContext])) { payload ⇒
          mutateOrFailures {
            ContextManager.createContext(payload)
          }
        } ~
        pathPrefix("contexts" / Segment) { name ⇒
          (get & pathEnd) {
            getOrFailures {
              ContextManager.getContextByName(name)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateObjectContext])) { payload ⇒
            mutateOrFailures {
              ContextManager.updateContextByName(name, payload)
            }
          }
        } ~
        pathPrefix("contexts") {
          (post & pathEnd & entity(as[CreateObjectContext])) { payload ⇒
            mutateOrFailures {
              ContextManager.createContext(payload)
            }
          }
        } ~
        pathPrefix(IntNumber) { contextId ⇒
          (get & pathEnd) {
            getOrFailures {
              ContextManager.getContext(contextId)
            }
          }
        }
      }
    }
  }
}
