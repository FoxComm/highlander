package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.ObjectSchemaPayloads._
import services.Authenticator.AuthData
import utils.aliases._
import services.objects.ObjectSchemasManager
import utils.http.CustomDirectives._
import utils.http.Http._

object ObjectRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("object" / "schemas") {
        (get & pathEnd) {
          getOrFailures {
            ObjectSchemasManager.getAllSchemas()
          }
        } ~
        (post & pathEnd & entity(as[CreateSchemaPayload])) { payload ⇒
          mutateOrFailures {
            ObjectSchemasManager.createSchema(payload)
          }
        } ~
        pathPrefix("byKind") {
          (get & path(Segment)) { kind ⇒
            getOrFailures {
              ObjectSchemasManager.getSchemasForKind(kind)
            }
          }
        } ~
        pathPrefix("byName" / Segment) { schemaName ⇒
          (get & pathEnd) {
            getOrFailures {
              ObjectSchemasManager.getSchema(schemaName)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateSchemaPayload])) { payload ⇒
            mutateOrFailures {
              ObjectSchemasManager.updateSchema(schemaName, payload)
            }
          }
        }
      }
    }
  }
}
