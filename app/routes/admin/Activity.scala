package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads._
import services.activity.{ActivityManager, TrailManager}
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._

object Activity {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("activities") {
        pathPrefix(IntNumber) { activityId ⇒
          (get & pathEnd) {
            goodOrFailures {
              ActivityManager.findById(activityId)
            }
          }
        }
      } ~
      pathPrefix("connections" / IntNumber) { connectionId ⇒
        (get & pathEnd) {
          goodOrFailures {
            TrailManager.findConnection(connectionId)
          }
        }
      } ~
      pathPrefix("trails" / Segment / Segment) { (dimension, objectId) ⇒
        (post & pathEnd) {
          entity(as[AppendActivity]) { payload ⇒
            goodOrFailures {
              TrailManager.appendActivityByObjectId(dimension, objectId, payload)
            }
          }
        }
      }
    }
  }
}
