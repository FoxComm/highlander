package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.utils.http.JsonSupport._
import phoenix.models.account.User
import phoenix.payloads.CatalogPayloads.{CreateCatalogPayload, UpdateCatalogPayload}
import phoenix.services.Authenticator.AuthData
import phoenix.services.catalog.CatalogManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._

object CatalogRoutes {
  def routes(implicit ec: EC, db: DB, auth: AU, apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("catalogs") {
        (post & pathEnd & entity(as[CreateCatalogPayload])) { payload ⇒
          mutateOrFailures {
            CatalogManager.createCatalog(payload)
          }
        } ~
        pathPrefix(IntNumber) { catalogId ⇒
          (get & pathEnd) {
            getOrFailures {
              CatalogManager.getCatalog(catalogId)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateCatalogPayload])) { payload ⇒
            mutateOrFailures {
              CatalogManager.updateCatalog(catalogId, payload)
            }
          }
        }
      }
    }
}
