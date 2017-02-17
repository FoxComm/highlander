package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import facades.NotificationFacade
import models.account.User
import payloads.CreateNotification
import services.NotificationManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object NotificationRoutes {

  def routes(implicit ec: EC, db: DB, mat: Mat, auth: AuthData[User]): Route = {
    activityContext(auth) { implicit ac ⇒
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
