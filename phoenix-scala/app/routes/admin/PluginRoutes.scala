package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.PluginPayloads._
import services.plugins.PluginsManager._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object PluginRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("plugins") {
        (get & pathEnd) {
          getOrFailures {
            listPlugins()
          }
        } ~
        pathPrefix("register") {
          (post & pathEnd & entity(as[RegisterPluginPayload])) { payload ⇒
            mutateOrFailures {
              registerPlugin(payload)
            }
          }
        } ~
        pathPrefix("settings") {
          (get & pathPrefix(Segment) & pathEnd) { name ⇒
            getOrFailures {
              listSettings(name)
            }
          } ~
          (post & pathPrefix(Segment) & pathEnd & entity(as[UpdateSettingsPayload])) {
            (name, payload) ⇒
              mutateOrFailures {
                updateSettings(name, payload)
              }
          }
        }
      }
    }
  }
}
