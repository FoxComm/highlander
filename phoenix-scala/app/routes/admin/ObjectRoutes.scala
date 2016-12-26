package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import services.Authenticator.AuthData
import utils.aliases._
import services.objects.ObjectSchemasManager
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object ObjectRoutes {
  def routes(implicit ec: EC,
             db: DB,
             auth: AuthData[User],
             tr: TracingRequest,
             trace: TracingExtensionImpl) = {
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
