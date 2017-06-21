package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import objectframework.payloads.ContextPayloads._
import objectframework.services.ObjectManager
import phoenix.models.account.User
import phoenix.models.product.ProductReference
import phoenix.payloads.ImagePayloads.{AlbumPayload, UpdateAlbumPositionPayload}
import phoenix.payloads.ProductPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.image.ImageManager
import phoenix.services.product.ProductManager
import phoenix.services.taxonomy.TaxonomyManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object ProductRoutes {

  private def productRoutes(
      productRef: ProductReference)(implicit ec: EC, db: DB, oc: OC, ac: AC, apis: Apis, auth: AU): Route =
    (get & pathEnd) {
      getOrFailures {
        ProductManager.getProduct(productRef, checkActive = false)
      }
    } ~
      (patch & pathEnd & entity(as[UpdateProductPayload])) { payload ⇒
        mutateOrFailures {
          ProductManager.updateProduct(productRef, payload)
        }
      } ~
      (delete & pathEnd) {
        mutateOrFailures {
          ProductManager.archiveByContextAndId(productRef)
        }
      } ~
      (pathPrefix("taxons") & get & pathEnd) {
        getOrFailures {
          TaxonomyManager.getAssignedTaxons(productRef)
        }
      } ~
      pathPrefix("albums") {
        (get & pathEnd) {
          getOrFailures {
            ImageManager.getAlbumsForProduct(productRef)
          }
        } ~
        (post & pathEnd & entity(as[AlbumPayload])) { payload ⇒
          mutateOrFailures {
            ImageManager.createAlbumForProduct(auth.model, productRef, payload)
          }
        } ~
        pathPrefix("position") {
          (post & pathEnd & entity(as[UpdateAlbumPositionPayload])) { payload ⇒
            mutateOrFailures {
              ImageManager.updateProductAlbumPosition(payload.albumId, productRef, payload.position)
            }
          }
        }
      }

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("products") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (post & pathEnd & entity(as[CreateProductPayload])) { payload ⇒
              mutateOrFailures {
                ProductManager.createProduct(auth.model, payload)
              }
            } ~
            pathPrefix(ProductRef) { productId ⇒
              productRoutes(productId)
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
