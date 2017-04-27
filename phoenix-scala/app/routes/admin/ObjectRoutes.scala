package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.ObjectSchemaPayloads._
import services.Authenticator.AuthData
import utils.aliases._
import services.objects.ObjectSchemasManager
import utils.http.CustomDirectives._
import utils.http.Http._

object ObjectRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("object" / "schemas") {
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
        (pathPrefix("byName") & path(Segment)) { schemaName ⇒
          (get & pathEnd) {
            getOrFailures {
              ObjectSchemasManager.getSchema(schemaName)
            }
          } ~
          (post & entity(as[UpdateObjectSchema])) { payload ⇒
            mutateOrFailures {
              ObjectSchemasManager.update(schemaName, payload)
            }
          }
        }
      }
    }
  }
}
