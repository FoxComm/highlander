package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import utils.http.JsonSupport._
import facades.AlbumImagesFacade
import facades.ImageFacade
import models.account.User
import payloads.ImagePayloads._
import services.image.ImageManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._

object ImageRoutes {
  def routes(implicit ec: EC, db: DB, am: Mat, auth: AuthData[User], apis: Apis): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("images") {
        pathPrefix("s3") {
          (post & pathEnd & entity(as[UploadImageByUrlPayload])) { payload ⇒
            goodOrFailures {
              ImageFacade.uploadImageToS3(payload)
            }
          }
        }
      } ~
      pathPrefix("albums") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[AlbumPayload])) { payload ⇒
            mutateOrFailures {
              ImageManager.createAlbum(payload, context)
            }
          } ~
          pathPrefix(IntNumber) { albumId ⇒
            (get & pathEnd) {
              getOrFailures {
                ImageManager.getAlbum(albumId, context)
              }
            } ~
            (patch & pathEnd & entity(as[AlbumPayload])) { payload ⇒
              mutateOrFailures {
                ImageManager.updateAlbum(albumId, payload, context)
              }
            } ~
            (delete & pathEnd) {
              mutateOrFailures {
                ImageManager.archiveByContextAndId(albumId, context)
              }
            } ~
            pathPrefix("images") {
              (post & pathEnd) {
                extractRequest { req ⇒
                  goodOrFailures {
                    AlbumImagesFacade.uploadImages(albumId, context, req)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
