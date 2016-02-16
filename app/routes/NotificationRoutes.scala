package routes

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import de.heikoseeberger.akkasse.EventStreamMarshalling._

import payloads._
import services.NotificationManager
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives._
import utils.Http._

object NotificationRoutes {

  def routes(implicit ec: ExecutionContext, db: Database, mat: Materializer, system: ActorSystem) = {

    activityContext() { implicit ac ⇒
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
