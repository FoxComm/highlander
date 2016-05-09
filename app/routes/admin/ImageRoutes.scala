package routes.admin

import java.io.File

import scala.concurrent.ExecutionContext
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import payloads._
import models.StoreAdmin
import services.image.ImageManager
import slick.driver.PostgresDriver.api._
import utils.http.Http._
import utils.http.CustomDirectives._

import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl._

import akka.stream.ActorMaterializer

object ImageRoutes {
  def routes(implicit ec: ExecutionContext, db: Database, am: ActorMaterializer, admin: StoreAdmin) = {
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
            } ~
            pathPrefix("images") {
              (post & pathEnd & extractRequest) { request ⇒
                goodOrFailures {
                  ImageManager.uploadImage(albumId, context, request.entity.dataBytes)
                }
              }
            }
          } 
        }
      }
    }
  }
}
