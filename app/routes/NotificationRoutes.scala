package routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._

import payloads._
import services.NotificationManager
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._
import utils.Config.Environment

object NotificationRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, system: ActorSystem, env: Environment) = {

    activityContext() { implicit ac ⇒
      pathPrefix("public") {
        pathPrefix("notifications") {
          (get & path(IntNumber) & pathEnd) { adminId ⇒
            complete {
              NotificationManager.streamByAdminId(adminId)
            }
          } ~
          (post & pathEnd & entity(as[CreateNotification])) { payload ⇒
            goodOrFailures {
              NotificationManager.createNotification(payload)
            }
          } ~
          (post & path(IntNumber / "last-seen" / IntNumber) & pathEnd) { (adminId, activityId) ⇒
            goodOrFailures {
              NotificationManager.updateLastSeen(adminId, activityId)
            }
          }
        }
      }
    }
  }
}
