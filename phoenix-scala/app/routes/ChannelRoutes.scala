package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.ChannelPayloads._
import services.Authenticator.AuthData
import services.ChannelManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object ChannelRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("channels") {
        (post & pathEnd & entity(as[CreateChannelPayload])) { payload ⇒
          mutateOrFailures {
            ChannelManager.createChannel(payload)
          }
        } ~
        pathPrefix(IntNumber) { channelId ⇒
          (get & pathEnd) {
            getOrFailures {
              ChannelManager.findById(channelId)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateChannelPayload])) { payload ⇒
            mutateOrFailures {
              ChannelManager.updateChannel(channelId, payload)
            }
          } ~
          (delete & pathEnd) {
            deleteOrFailures {
              ChannelManager.archiveChannel(channelId)
            }
          }
        }
      }
    }
  }
}
