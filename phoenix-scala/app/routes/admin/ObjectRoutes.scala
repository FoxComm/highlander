package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
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
      }
    }
  }
}
