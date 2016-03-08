package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import services.ProductManager
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

import payloads.{CreateProductForm, UpdateProductForm, CreateProductShadow, 
  UpdateProductShadow, CreateProductContext, UpdateProductContext,
  CreateFullProduct, UpdateFullProduct}


object ProductRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, admin: StoreAdmin, apis: Apis) = {

      activityContext(admin) { implicit ac ⇒

        pathPrefix("products") {
          pathPrefix("full") {
            pathPrefix(Segment / IntNumber / "baked") { (context, productId) ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getIlluminatedFullProduct(productId, context)
                }
              }
            } ~ 
            pathPrefix(Segment / IntNumber) { (context, productId)  ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getFullProduct(productId, context)
                }
              } ~ 
              (patch & pathEnd & entity(as[UpdateFullProduct])) { payload ⇒
                goodOrFailures {
                  ProductManager.updateFullProduct(productId, payload, context)
                }
              } 
            } ~
            pathPrefix(Segment) { (context)  ⇒
              (post & pathEnd & entity(as[CreateFullProduct])) { payload ⇒
                goodOrFailures {
                  ProductManager.createFullProduct(payload, context)
                }
              } 
            }
          } ~
          pathPrefix(IntNumber / "form") { productId ⇒
            (get & pathEnd) {
              goodOrFailures {
                ProductManager.getForm(productId)
              }
            } ~ 
            (patch & pathEnd & entity(as[UpdateProductForm])) { payload ⇒
              goodOrFailures {
                ProductManager.updateForm(productId, payload)
              }
            } 
          } ~
          pathPrefix("form") { 
            (post & pathEnd & entity(as[CreateProductForm])) { payload ⇒
              goodOrFailures {
                ProductManager.createForm(payload)
              }
            } 
          } ~
          pathPrefix(Segment / IntNumber / "baked") { (context, productId) ⇒
            (get & pathEnd) {
              goodOrFailures {
                ProductManager.getIlluminatedProduct(productId, context)
              }
            }
          } ~
          pathPrefix(Segment / IntNumber / "shadow") { (context, productId)  ⇒
            (get & pathEnd) {
              goodOrFailures {
                ProductManager.getShadow(productId, context)
              }
            } ~ 
            (patch & pathEnd & entity(as[UpdateProductShadow])) { payload ⇒
              goodOrFailures {
                ProductManager.updateShadow(productId, payload, context)
              }
            } 
          } ~
          pathPrefix(Segment/ IntNumber / "shadow") { (context, productId)  ⇒
            (post & pathEnd & entity(as[CreateProductShadow])) { payload ⇒
              goodOrFailures {
                ProductManager.createShadow(productId, payload, context)
              }
            } 
          } ~
          pathPrefix("contexts" / Segment) { name ⇒
            (get & pathEnd) {
              goodOrFailures {
                ProductManager.getContextByName(name)
              }
            } ~ 
            (patch & pathEnd & entity(as[UpdateProductContext])) { payload ⇒
              goodOrFailures {
                ProductManager.updateContextByName(name, payload)
              }
            } 
          } ~
          pathPrefix("contexts") { 
            (post & pathEnd & entity(as[CreateProductContext])) { payload ⇒
              goodOrFailures {
                ProductManager.createContext(payload)
              }
            } 
          }
        }
      }
  }
}
