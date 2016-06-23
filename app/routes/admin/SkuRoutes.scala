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
            mutateGoodOrFailures {
              SkuManager.createSku(context, payload)
            }
          } ~
          pathPrefix(Segment) { code ⇒
            (get & pathEnd) {
              getGoodOrFailures {
                SkuManager.getSku(context, code)
              }
            } ~
            (patch & pathEnd & entity(as[SkuPayload])) { payload ⇒
              mutateGoodOrFailures {
                SkuManager.updateSku(context, code, payload)
              }
            } ~
            pathPrefix("albums") {
              (get & pathEnd) {
                getGoodOrFailures {
                  ImageManager.getAlbumsForSku(code, context)
                }
              } ~
              (post & pathEnd & entity(as[CreateAlbumPayload])) { payload ⇒
                mutateGoodOrFailures {
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
