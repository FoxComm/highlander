package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import facades.NotificationFacade
import models.account.User
import payloads.CreateNotification
import services.NotificationManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object NotificationRoutes {

  def routes(implicit ec: EC,
             db: DB,
             mat: Mat,
             auth: AuthData[User],
             tr: TracingRequest,
             trace: TracingExtensionImpl) = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("notifications") {
        (get & pathEnd) {
          complete {
            NotificationFacade.streamByAdminId(auth.account.id)
          }
        } ~
        (post & pathEnd & entity(as[CreateNotification])) { payload ⇒
          mutateOrFailures {
            NotificationManager.createNotification(payload)
          }
        } ~
        (post & path("last-seen" / IntNumber) & pathEnd) { activityId ⇒
          mutateOrFailures {
            NotificationManager.updateLastSeen(auth.account.id, activityId)
          }
        }
      }
    }
  }
}
