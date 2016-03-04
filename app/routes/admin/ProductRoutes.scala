package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import services.ProductManager
import services.Authenticator.{AsyncAuthenticator, requireAdminAuth}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

import payloads.{CreateProductForm, UpdateProductForm, CreateProductShadow, 
  UpdateProductShadow, CreateProductContext, UpdateProductContext,
  CreateFullProductForm, UpdateFullProductForm, CreateFullProductShadow, 
  UpdateFullProductShadow}


object ProductRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

      requireAdminAuth(storeAdminAuth) { admin ⇒

        activityContext(admin) { implicit ac ⇒

          pathPrefix("products") {
            pathPrefix("full") {
              pathPrefix("forms" / IntNumber) { id ⇒
                (get & pathEnd) {
                  goodOrFailures {
                    ProductManager.getFullForm(id)
                  }
                } ~ 
                (patch & pathEnd & entity(as[UpdateFullProductForm])) { payload ⇒
                  goodOrFailures {
                    ProductManager.updateFullForm(id, payload)
                  }
                } 
              } ~
              pathPrefix("forms") { 
                (post & pathEnd & entity(as[CreateFullProductForm])) { payload ⇒
                  goodOrFailures {
                    ProductManager.createFullForm(payload)
                  }
                } 
              } ~
              pathPrefix("shadows" / Segment / IntNumber) { (context, id)  ⇒
                (get & pathEnd) {
                  goodOrFailures {
                    ProductManager.getFullShadow(id, context)
                  }
                } ~ 
                (patch & pathEnd & entity(as[UpdateFullProductShadow])) { payload ⇒
                  goodOrFailures {
                    ProductManager.updateFullShadow(id, payload, context)
                  }
                } 
              } ~
              pathPrefix("shadows" / Segment) { (context)  ⇒
                (post & pathEnd & entity(as[CreateFullProductShadow])) { payload ⇒
                  goodOrFailures {
                    ProductManager.createFullShadow(payload, context)
                  }
                } 
              } ~ 
              pathPrefix("illuminated" / Segment / IntNumber) { (context, id) ⇒
                (get & pathEnd) {
                  goodOrFailures {
                    ProductManager.getIlluminatedFullProduct(id, context)
                  }
                }
              } 
            } ~
            pathPrefix("forms" / IntNumber) { id ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getForm(id)
                }
              } ~ 
              (patch & pathEnd & entity(as[UpdateProductForm])) { payload ⇒
                goodOrFailures {
                  ProductManager.updateForm(id, payload)
                }
              } 
            } ~
            pathPrefix("forms") { 
              (post & pathEnd & entity(as[CreateProductForm])) { payload ⇒
                goodOrFailures {
                  ProductManager.createForm(payload)
                }
              } 
            } ~
            pathPrefix("shadows" / Segment / IntNumber) { (context, id)  ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getShadow(id, context)
                }
              } ~ 
              (patch & pathEnd & entity(as[UpdateProductShadow])) { payload ⇒
                goodOrFailures {
                  ProductManager.updateShadow(id, payload, context)
                }
              } 
            } ~
            pathPrefix("shadows" / Segment) { (context)  ⇒
              (post & pathEnd & entity(as[CreateProductShadow])) { payload ⇒
                goodOrFailures {
                  ProductManager.createShadow(payload, context)
                }
              } 
            } ~
            pathPrefix("illuminated" / Segment / IntNumber) { (context, id) ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getIlluminatedProduct(id, context)
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
}
