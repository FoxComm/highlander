package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import payloads.{CreateDiscount, UpdateDiscount}
import services.discount.DiscountManager
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives._
import utils.Http._

object DiscountRoutes {

  def routes(implicit ec: ExecutionContext, db: Database, admin: StoreAdmin) = {

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
          pathPrefix(Segment) { (context)  ⇒
            (post & pathEnd & entity(as[CreateDiscount])) { payload ⇒
              goodOrFailures {
                DiscountManager.create(payload, context)
              }
            } ~ 
            pathPrefix(IntNumber) { id ⇒ 
              (get & path("baked")) {
                goodOrFailures {
                  DiscountManager.getIlluminated(id, context)
                }
              } ~
              (get & pathEnd) {
                goodOrFailures {
                  DiscountManager.get(id, context)
                }
              } ~
              (patch & pathEnd & entity(as[UpdateDiscount])) { payload ⇒
                goodOrFailures {
                  DiscountManager.update(id, payload, context)
                }
              } 
            }
          }
        }
      }
  }
}
