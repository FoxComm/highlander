package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.product.ProductReference
import payloads.ContextPayloads._
import payloads.ImagePayloads.{AlbumPayload, UpdateAlbumPositionPayload}
import payloads.ProductPayloads._
import services.image.ImageManager
import services.objects.ObjectManager
import services.product.ProductManager
import services.taxonomy.TaxonomyManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object ProductRoutes {

  def productRoutes(productRef: ProductReference)(implicit ec: EC,
                                                  db: DB,
                                                  oc: OC,
                                                  ac: AC,
                                                  auth: AU,
                                                  tr: TracingRequest,
                                                  trace: TracingExtensionImpl): Route = {
    (get & pathEnd) {
      getOrFailures {
        ProductManager.getProduct(productRef)
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
  }

  def routes(implicit ec: EC, db: DB, auth: AU, tr: TracingRequest, trace: TracingExtensionImpl) = {

    activityContext(auth.model) { implicit ac ⇒
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
}
