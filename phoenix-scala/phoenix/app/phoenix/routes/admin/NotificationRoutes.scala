package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkasse.scaladsl.marshalling.EventStreamMarshalling._
import phoenix.facades.NotificationFacade
import phoenix.models.account.User
import phoenix.payloads.CreateNotification
import phoenix.services.Authenticator.AuthData
import phoenix.services.NotificationManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object NotificationRoutes {

  def routes(implicit ec: EC,
             db: DB,
             mat: Mat,
             auth: AuthData[User],
             apis: Apis,
             system: akka.actor.ActorSystem): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("notifications") {
        (get & pathEnd) {
          complete {
            NotificationFacade.streamForCurrentAdmin()
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
