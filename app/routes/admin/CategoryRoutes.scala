package routes.admin

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import payloads._
import models.StoreAdmin
import services.category.CategoryManager
import slick.driver.PostgresDriver.api._
import utils.http.Http._
import utils.http.CustomDirectives._

object CategoryRoutes {

  def routes(implicit ec: ExecutionContext, db: Database, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒

      pathPrefix("categories") {
        pathPrefix(Segment / IntNumber) { (context, categoryId) ⇒
          (get & pathEnd) {
            goodOrFailures {
              CategoryManager.getCategory(categoryId, context)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateFullCategory])) { payload ⇒
            goodOrFailures {
              CategoryManager.updateCategory(admin, categoryId, payload, context)
            }
          }
        } ~
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreateFullCategory])) { payload ⇒
            goodOrFailures {
              CategoryManager.createCategory(admin, payload, context)
            }
          }
        } ~
        pathPrefix(IntNumber / "form") { categoryId ⇒
          (get & pathEnd) {
            goodOrFailures {
              CategoryManager.getForm(categoryId)
            }
          }
        } ~
        pathPrefix(Segment / IntNumber / "baked") { (context, categoryId) ⇒
          (get & pathEnd) {
            goodOrFailures {
              CategoryManager.getIlluminatedCategory(categoryId, context)
            }
          }
        } ~
        pathPrefix(Segment / IntNumber / "shadow") { (context, categoryId) ⇒
          (get & pathEnd) {
            goodOrFailures {
              CategoryManager.getShadow(categoryId, context)
            }
          }
        }
      }
    }
  }
}
