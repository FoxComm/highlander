package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import facades.NotificationFacade
import models.account.User
import payloads.CreateNotification
import services.NotificationManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object NotificationRoutes {

  def routes(implicit ec: EC, db: DB, mat: Mat, admin: User) = {
    activityContext(admin) { implicit ac ⇒
      pathPrefix("notifications") {
        (get & pathEnd) {
          complete {
            NotificationFacade.streamByAdminId(admin.accountId)
          }
        } ~
        (post & pathEnd & entity(as[CreateNotification])) { payload ⇒
          mutateOrFailures {
            NotificationManager.createNotification(payload)
          }
        } ~
        (post & path("last-seen" / IntNumber) & pathEnd) { activityId ⇒
          mutateOrFailures {
            NotificationManager.updateLastSeen(admin.accountId, activityId)
          }
        }
      }
    }
  }
}
