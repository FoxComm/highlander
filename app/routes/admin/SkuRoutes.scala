package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.ImagePayloads.CreateAlbumPayload
import payloads.SkuPayloads._
import services.image.ImageManager
import services.inventory.SkuManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object SkuRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("skus") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[SkuPayload])) { payload ⇒
            mutateOrFailures {
              SkuManager.createSku(context, payload)
            }
          } ~
          pathPrefix(Segment) { code ⇒
            (get & pathEnd) {
              getOrFailures {
                SkuManager.getSku(context, code)
              }
            } ~
            (patch & pathEnd & entity(as[SkuPayload])) { payload ⇒
              mutateOrFailures {
                SkuManager.updateSku(context, code, payload)
              }
            } ~
            pathPrefix("albums") {
              (get & pathEnd) {
                getOrFailures {
                  ImageManager.getAlbumsForSku(code, context)
                }
              } ~
              (post & pathEnd & entity(as[CreateAlbumPayload])) { payload ⇒
                mutateOrFailures {
                  ImageManager.createAlbumForSku(admin, code, payload, context)
                }
              }
            }
          }
        }
      }
    }
  }
}
