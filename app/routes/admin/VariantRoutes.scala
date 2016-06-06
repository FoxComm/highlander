package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.VariantPayloads.{CreateVariantPayload, CreateVariantValuePayload}
import services.variant.VariantManager
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.http.Http._
import utils.http.CustomDirectives._

object VariantRoutes {

  def routes(implicit ec: ExecutionContext, db: Database, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("variants") {
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[CreateVariantPayload])) { payload ⇒
            goodOrFailures {
              VariantManager.createVariant(context, payload)
            }
          } ~
          pathPrefix(IntNumber) { variantId ⇒
            pathPrefix("values") {
              (post & pathEnd & entity(as[CreateVariantValuePayload])) { payload ⇒
                goodOrFailures {
                  VariantManager.createVariantValue(context, variantId, payload)
                }
              }
            }
          }
        }
      }
    }
  }
}
