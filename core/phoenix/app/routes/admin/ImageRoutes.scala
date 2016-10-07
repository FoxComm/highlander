package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
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
  def routes(implicit ec: EC, db: DB, am: Mat, auth: AuthData[User], apis: Apis) = {
    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("albums") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[CreateAlbumPayload])) { payload ⇒
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
            (patch & pathEnd & entity(as[UpdateAlbumPayload])) { payload ⇒
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
                    ImageFacade.uploadImage(albumId, context, req)
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
