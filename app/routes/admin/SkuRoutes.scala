package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import payloads.ImagePayloads.CreateAlbumPayload
import payloads.SkuPayloads._
import services.image.ImageManager
import services.inventory.SkuManager
import slick.driver.PostgresDriver.api._
import utils.http.Http._
import utils.http.CustomDirectives._

object SkuRoutes {

  def routes(implicit ec: ExecutionContext, db: Database, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("skus") {
        pathPrefix("full") {
          pathPrefix(Segment / Segment) { (context, code) ⇒
            (get & pathEnd) {
              goodOrFailures {
                SkuManager.getFullSkuByContextName(code, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdateFullSku])) { payload ⇒
              goodOrFailures {
                SkuManager.updateFullSku(admin, code, payload, context)
              }
            }
          } ~
          pathPrefix(Segment) { (context) ⇒
            (post & pathEnd & entity(as[CreateFullSku])) { payload ⇒
              goodOrFailures {
                SkuManager.createFullSku(admin, payload, context)
              }
            }
          }
        } ~
        pathPrefix("forms" / Segment) { code ⇒
          (get & pathEnd) {
            goodOrFailures {
              SkuManager.getForm(code)
            }
          }
        } ~
        pathPrefix("shadows" / Segment / Segment) { (context, code) ⇒
          (get & pathEnd) {
            goodOrFailures {
              SkuManager.getShadow(code, context)
            }
          }
        } ~
        pathPrefix("illuminated" / Segment / Segment) { (context, code) ⇒
          (get & pathEnd) {
            goodOrFailures {
              SkuManager.getIlluminatedSku(code, context)
            }
          }
        } ~
        pathPrefix(Segment / Segment / "albums") { (context, code) ⇒
          (get & pathEnd) {
            goodOrFailures {
              ImageManager.getAlbumsForSku(code, context)
            }
          } ~
          (post & pathEnd & entity(as[CreateAlbumPayload])) { payload ⇒
            goodOrFailures {
              ImageManager.createAlbumForSku(admin, code, payload, context)
            }
          }
        } ~
        pathPrefix(Segment) { context ⇒
          (post & pathEnd & entity(as[CreateSkuPayload])) { payload ⇒
            goodOrFailures {
              SkuManager.createSku(context, payload)
            }
          }
        }
      }
    }
  }
}
