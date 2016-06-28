package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.ContextPayloads._
import payloads.ImagePayloads.CreateAlbumPayload
import payloads.ProductPayloads._
import services.image.ImageManager
import services.objects.ObjectManager
import services.product.ProductManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object ProductRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("products") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName)(db, ec) { implicit context ⇒
            (post & pathEnd & entity(as[CreateProductPayload])) { payload ⇒
              mutateOrFailures {
                ProductManager.createProduct(payload)
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
                  ProductManager.updateProduct(productId, payload)
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
              }
            }
          }
        } ~
        pathPrefix("contexts" / Segment) { name ⇒
          (get & pathEnd) {
            goodOrFailures {
              ObjectManager.getContextByName(name)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateObjectContext])) { payload ⇒
            goodOrFailures {
              ObjectManager.updateContextByName(name, payload)
            }
          }
        } ~
        pathPrefix("contexts") {
          (post & pathEnd & entity(as[CreateObjectContext])) { payload ⇒
            goodOrFailures {
              ObjectManager.createContext(payload)
            }
          }
        } ~
        pathPrefix(IntNumber / "contexts") { formId ⇒
          (get & pathEnd) {
            goodOrFailures {
              ProductManager.getContextsForProduct(formId)
            }
          }
        }
      }
    }
  }
}
