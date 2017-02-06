package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import models.account.User
import payloads.ActivityTrailPayloads.AppendActivity
import services.activity.{ActivityManager, TrailManager}
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object Activity {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("activities") {
        pathPrefix(IntNumber) { activityId ⇒
          (get & pathEnd) {
            getOrFailures {
              ActivityManager.findById(activityId)
            }
          }
        }
      } ~
      pathPrefix("connections" / IntNumber) { connectionId ⇒
        (get & pathEnd) {
          getOrFailures {
            TrailManager.findConnection(connectionId)
          }
        }
      } ~
      pathPrefix("trails" / Segment / Segment) { (dimension, objectId) ⇒
        (post & pathEnd) {
          entity(as[AppendActivity]) { payload ⇒
            mutateOrFailures {
              TrailManager.appendActivityByObjectId(dimension, objectId, payload)
            }
          }
        }
      }
    }
  }
}
