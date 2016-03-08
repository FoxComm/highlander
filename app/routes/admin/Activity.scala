package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads._
import services.activity.{ActivityManager, TrailManager}
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._


object Activity {

  def routes(implicit ec: EC, db: DB, mat: Materializer, admin: StoreAdmin, apis: Apis) = {

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
