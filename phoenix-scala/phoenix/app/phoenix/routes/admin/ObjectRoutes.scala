package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import objectframework.payloads.ObjectSchemaPayloads._
import objectframework.services.ObjectSchemasManager
import phoenix.models.account.User
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object ObjectRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
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
