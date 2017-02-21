package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.PluginPayloads._
import services.plugins.PluginsManager._
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object PluginRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
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
