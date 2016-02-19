package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import payloads._
import services.activity.{ActivityManager, TrailManager}
import services.Authenticator.{AsyncAuthenticator, requireAdminAuth}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._

object Activity {

  def routes(implicit ec: ExecutionContext, db: Database, 
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    requireAdminAuth(storeAdminAuth) { admin ⇒
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
}
