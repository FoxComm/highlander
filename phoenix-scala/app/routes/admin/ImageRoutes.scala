package routes.admin

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import utils.http.JsonSupport._
import facades.AlbumImagesFacade
import facades.ImageFacade
import failures.ImageFailures.ImageNotFoundInPayload
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
              extractRequestContext { ctx ⇒
                implicit val materializer = ctx.materializer
                implicit val ec           = ctx.executionContext
                (post & pathEnd & entityOr(as[Multipart.FormData], ImageNotFoundInPayload)) {
                  formData ⇒
                    goodOrFailures {
                      AlbumImagesFacade.uploadImagesFromMultipart(albumId, context, formData)
                    }
                } ~
                (path("byUrl") & post & entity(as[ImagePayload])) { payload ⇒
                  goodOrFailures {
                    AlbumImagesFacade.uploadImagesFromPayload(albumId, context, payload)
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
