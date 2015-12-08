package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import slick.driver.PostgresDriver.api._
import scala.concurrent.{ExecutionContext, Future}

import models.StoreAdmin
import models.activity.ActivityContext

import services.activity.ActivityManager
import services.activity.TrailManager

import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._

import payloads.AppendActivity

object Activity {

  def routes(implicit ec: ExecutionContext, db: Database, 
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("activities" / IntNumber) { activityId ⇒ 
        (get & pathEnd) { 
          goodOrFailures {
            ActivityManager.findById(activityId)
          }
        } 
      } ~
      pathPrefix("trails" / Segment/ IntNumber) { (dimension, objectId) ⇒ 
        (get & pathEnd) { 
          activityContext(admin) { implicit ac ⇒ 
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
