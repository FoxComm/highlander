package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import payloads._
import models.StoreAdmin
import services.image.ImageManager
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.http.Http._
import utils.http.CustomDirectives._

object ImageRoutes {
  def routes(implicit ec: ExecutionContext, db: Database, admin: StoreAdmin) = {
    activityContext(admin) { implicit ac ⇒  
      pathPrefix("albums") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[AlbumPayload])) { payload ⇒
            goodOrFailures {
              ImageManager.createAlbum(payload, context)
            }
          } ~
          pathPrefix(IntNumber) { albumId ⇒
            (get & pathEnd) {
              goodOrFailures {
                ImageManager.getAlbum(albumId, context)
              }
            } ~
            (patch & pathEnd & entity(as[AlbumPayload])) { payload ⇒
              goodOrFailures {
                ImageManager.updateAlbum(albumId, payload, context)
              }
            }
          }
        }
      }
    }
  }
}
