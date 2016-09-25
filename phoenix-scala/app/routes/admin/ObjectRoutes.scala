package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import utils.aliases._
import services.objects.ObjectSchemasManager
import utils.http.CustomDirectives._
import utils.http.Http._

object ObjectRoutes {
  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("object" / "schemas") {
        path(Segment) { schemaName ⇒
          getOrFailures {
            ObjectSchemasManager.getSchema(schemaName)
          }
        }
      }
    }
  }
}
