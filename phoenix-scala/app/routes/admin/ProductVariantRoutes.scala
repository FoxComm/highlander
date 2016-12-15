package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductVariantPayloads._
import services.image.ImageManager
import services.inventory.ProductVariantManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object ProductVariantRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]) = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("skus") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (post & pathEnd & entity(as[ProductVariantPayload])) { payload ⇒
              mutateOrFailures {
                ProductVariantManager.create(auth.model, payload)
              }
            } ~
            pathPrefix(Segment) { code ⇒
              (get & pathEnd) {
                getOrFailures {
                  ProductVariantManager.getBySkuCode(code)
                }
              } ~
              (patch & pathEnd & entity(as[ProductVariantPayload])) { payload ⇒
                mutateOrFailures {
                  ProductVariantManager.update(auth.model, code, payload)
                }
              } ~
              (delete & pathEnd) {
                mutateOrFailures {
                  ProductVariantManager.archiveByCode(code)
                }
              } ~
              pathPrefix("albums") {
                (get & pathEnd) {
                  getOrFailures {
                    ImageManager.getAlbumsForSku(code)
                  }
                } ~
                (post & pathEnd & entity(as[AlbumPayload])) { payload ⇒
                  mutateOrFailures {
                    ImageManager.createAlbumForSku(auth.model, code, payload)
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
