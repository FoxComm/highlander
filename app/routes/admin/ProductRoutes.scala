package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import payloads._
import models.StoreAdmin
import services.ObjectManager
import services.ProductManager
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

object ProductRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, admin: StoreAdmin, apis: Apis) = {

      activityContext(admin) { implicit ac ⇒

        pathPrefix("products") {
          pathPrefix("full") {
            pathPrefix(Segment / IntNumber / "baked") { (context, productId) ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getIlluminatedFullProductByContextName(productId, context)
                }
              }
            } ~ 
            pathPrefix(Segment / IntNumber / "baked" / IntNumber) { (context, productId, commitId) ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getIlluminatedFullProductAtCommit(productId, context, commitId)
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
          }
        }
      }
  }
}
