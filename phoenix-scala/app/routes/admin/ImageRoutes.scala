package routes.admin

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller

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
import utils.http.JsonSupport._

object ImageRoutes {
  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis, sys: ActorSystem): Route = {
    activityContext(auth) { implicit ac ⇒
      pathPrefix("images" / Segment) { context ⇒
        extractRequestContext { ctx ⇒
          implicit val materializer = ctx.materializer
          implicit val ec           = ctx.executionContext
          import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._

          (post & pathEnd & entityOr(as[Multipart.FormData], ImageNotFoundInPayload)) { formData ⇒
            mutateOrFailures {
              AlbumImagesFacade.uploadImagesFromMultiPart(context, formData)
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
              extractRequestContext { ctx ⇒
                implicit val materializer = ctx.materializer
                implicit val ec           = ctx.executionContext
                import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers._

                (post & pathEnd & entityOr(as[Multipart.FormData], ImageNotFoundInPayload)) {
                  formData ⇒
                    mutateOrFailures {
                      AlbumImagesFacade.uploadImagesFromMultipartToAlbum(albumId,
                                                                         context,
                                                                         formData)
                    }
                } ~
                (path("byUrl") & post & entity(as[ImagePayload])) { payload ⇒
                  mutateOrFailures {
                    AlbumImagesFacade.uploadImagesFromPayloadToAlbum(albumId, context, payload)
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
