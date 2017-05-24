package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.utils.http.JsonSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._
import phoenix.facades.NotificationFacade
import phoenix.models.account.User
import phoenix.payloads.CreateNotification
import phoenix.services.NotificationManager
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._

object NotificationRoutes {

  def routes(implicit ec: EC,
             db: DB,
             mat: Mat,
             auth: AuthData[User],
             system: akka.actor.ActorSystem): Route = {
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
        (post & path("last-seen" / IntNumber) & pathEnd) { notificationId ⇒
          mutateOrFailures {
            NotificationManager.updateLastSeen(auth.account.id, notificationId)
          }
        }
      }
    }
  }
}
