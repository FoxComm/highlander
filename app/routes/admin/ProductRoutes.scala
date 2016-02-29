package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import models.StoreAdmin
import services.ProductManager
import services.Authenticator.{AsyncAuthenticator, requireAdminAuth}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._

object ProductRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

      requireAdminAuth(storeAdminAuth) { admin ⇒

        activityContext(admin) { implicit ac ⇒

          pathPrefix("products") {
            pathPrefix("forms" / IntNumber) { id ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getForm(id)
                }
              }
            } ~
            pathPrefix("shadows" / Segment / IntNumber) { (context, id)  ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getShadow(id, context)
                }
              }
            } ~
            pathPrefix("illuminated" / Segment / IntNumber) { (context, id) ⇒
              (get & pathEnd) {
                goodOrFailures {
                  ProductManager.getIlluminatedProduct(id, context)
                }
              }
            }
          }
        }
      }
  }
}
