package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.activity.ActivityContext
import services.activity.ActivityManager
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._

import scala.concurrent.{ExecutionContext, Future}

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
      }
    }
  }
}
