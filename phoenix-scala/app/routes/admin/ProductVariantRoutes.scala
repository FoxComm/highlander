package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import models.account.User
import payloads.ImagePayloads.AlbumPayload
import payloads.ProductVariantPayloads._
import services.Authenticator.AuthData
import services.image.ImageManager
import services.inventory.ProductVariantManager
import utils.aliases._
import utils.apis.Apis
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.http.JsonSupport._

object ProductVariantRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("product-variants") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            pathPrefix(IntNumber) { variantId ⇒
              (get & pathEnd) {
                getOrFailures {
                  ProductVariantManager.getByFormId(variantId)
                }
              } ~
              (patch & pathEnd & entity(as[ProductVariantPayload])) { payload ⇒
                mutateOrFailures {
                  ProductVariantManager.update(auth.model, variantId, payload)
                }
              } ~
              (delete & pathEnd) {
                mutateOrFailures {
                  ProductVariantManager.archive(variantId)
                }
              } ~
              pathPrefix("albums") {
                (get & pathEnd) {
                  getOrFailures {
                    ImageManager.getAlbumsForVariant(variantId)
                  }
                } ~
                (post & pathEnd & entity(as[AlbumPayload])) { payload ⇒
                  mutateOrFailures {
                    ImageManager.createAlbumForVariant(auth.model, variantId, payload)
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
