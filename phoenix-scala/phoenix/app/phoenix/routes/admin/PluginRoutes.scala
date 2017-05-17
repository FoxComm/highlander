package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.utils.http.JsonSupport._
import phoenix.models.account.User
import phoenix.payloads.PluginPayloads._
import phoenix.services.plugins.PluginsManager._
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._

object PluginRoutes {

  def routes(implicit ec: EC, db: DB, auth: AU): Route = {
    activityContext(auth) { implicit ac ⇒
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
        (pathPrefix("settings") & pathPrefix(Segment)) { pluginName ⇒
          (get & pathEnd) {
            getOrFailures {
              listSettings(pluginName)
            }
          } ~
          (get & path("detailed")) {
            getOrFailures {
              getSettingsWithSchema(pluginName)
            }
          } ~
          (post & pathEnd & entity(as[UpdateSettingsPayload])) { payload ⇒
            mutateOrFailures {
              updateSettings(pluginName, payload)
            }
          }
        }
      }
    }
  }
}
