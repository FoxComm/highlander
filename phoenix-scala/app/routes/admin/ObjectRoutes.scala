package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import models.account.User
import payloads.GenericObjectPayloads._
import services.Authenticator.AuthData
import services.objects.ObjectSchemasManager
import services.objects.ObjectManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.http.JsonSupport._

object ObjectRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("object") {
        pathPrefix("schemas") {
          (get & pathEnd) {
            getOrFailures {
              ObjectSchemasManager.getAllSchemas()
            }
          } ~
          pathPrefix("byKind") {
            (get & path(Segment)) { kind ⇒
              getOrFailures {
                ObjectSchemasManager.getSchemasForKind(kind)
              }
            }
          } ~
          (get & pathPrefix("byName") & path(Segment)) { schemaName ⇒
            getOrFailures {
              ObjectSchemasManager.getSchema(schemaName)
            }
          }
        } ~
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreateGenericObject])) { payload ⇒
            mutateOrFailures {
              ObjectManager.create(payload, context)
            }
          } ~
          pathPrefix(IntNumber) { id ⇒
            (get & pathEnd) {
              getOrFailures {
                ObjectManager.getIlluminated(id, context)
              }
            }
            (patch & pathEnd & entity(as[UpdateGenericObject])) { payload ⇒
              mutateOrFailures {
                ObjectManager.update(id, payload, context)
              }
            }
          }
        }
      }
    }
  }
}
