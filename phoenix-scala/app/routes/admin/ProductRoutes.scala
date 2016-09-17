package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.ContextPayloads._
import payloads.ImagePayloads.{UpdateAlbumPositionPayload, CreateAlbumPayload}
import payloads.ProductPayloads._
import services.image.ImageManager
import services.objects.ObjectManager
import services.product.ProductManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object ProductRoutes {

  def routes(implicit ec: EC, db: DB, admin: User) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("products") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (post & pathEnd & entity(as[CreateProductPayload])) { payload ⇒
              mutateOrFailures {
                ProductManager.createProduct(admin, payload)
              }
            } ~
            pathPrefix(IntNumber) { productId ⇒
              (get & pathEnd) {
                getOrFailures {
                  ProductManager.getProduct(productId)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateProductPayload])) { payload ⇒
                mutateOrFailures {
                  ProductManager.updateProduct(admin, productId, payload)
                }
              } ~
              (delete & pathEnd) {
                mutateOrFailures {
                  ProductManager.archiveByContextAndId(productId)
                }
              }
            } ~
            pathPrefix(IntNumber / "albums") { productId ⇒
              (get & pathEnd) {
                getOrFailures {
                  ImageManager.getAlbumsForProduct(productId)
                }
              } ~
              (post & pathEnd & entity(as[CreateAlbumPayload])) { payload ⇒
                mutateOrFailures {
                  ImageManager.createAlbumForProduct(admin, productId, payload, contextName)
                }
              } ~
              pathPrefix("position") {
                (post & pathEnd & entity(as[UpdateAlbumPositionPayload])) { payload ⇒
                  mutateOrFailures {
                    ImageManager.updateProductAlbumPosition(payload.albumId,
                                                            productId,
                                                            payload.position)
                  }
                }
              }
            }
          }
        } ~
        pathPrefix("contexts" / Segment) { name ⇒
          (get & pathEnd) {
            getOrFailures {
              ObjectManager.getContextByName(name)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateObjectContext])) { payload ⇒
            mutateOrFailures {
              ObjectManager.updateContextByName(name, payload)
            }
          }
        } ~
        pathPrefix("contexts") {
          (post & pathEnd & entity(as[CreateObjectContext])) { payload ⇒
            mutateOrFailures {
              ObjectManager.createContext(payload)
            }
          }
        } ~
        pathPrefix(IntNumber / "contexts") { formId ⇒
          (get & pathEnd) {
            getOrFailures {
              ProductManager.getContextsForProduct(formId)
            }
          }
        }
      }
    }
  }
}
