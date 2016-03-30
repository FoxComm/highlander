package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import services.discount.DiscountManager
import services.Authenticator.{AsyncAuthenticator, requireAuth}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Apis
import utils.Http._
import utils.CustomDirectives._

object DiscountRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, admin: StoreAdmin, apis: Apis) = {

      activityContext(admin) { implicit ac ⇒

        pathPrefix("discounts") {
          pathPrefix("forms" / IntNumber) { id ⇒
            (get & pathEnd) {
              goodOrFailures {
                DiscountManager.getForm(id)
              }
            }
          } ~
          pathPrefix("shadows" / Segment / IntNumber) { (context, id)  ⇒
            (get & pathEnd) {
              goodOrFailures {
                DiscountManager.getShadow(id, context)
              }
            }
          } ~
          pathPrefix(Segment / IntNumber) { (context, id) ⇒
            (get & pathEnd) {
              goodOrFailures {
                DiscountManager.getIlluminatedDiscount(id, context)
              }
            }
          } 
        }
      }
  }
}
