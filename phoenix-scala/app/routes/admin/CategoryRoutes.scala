package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import payloads.CategoryPayloads._
import services.category.CategoryManager
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object CategoryRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]) = {

    activityContext(auth.model) { implicit ac ⇒
      pathPrefix("categories") {
        pathPrefix(Segment / IntNumber) { (context, categoryId) ⇒
          (get & pathEnd) {
            getOrFailures {
              CategoryManager.getCategory(categoryId, context)
            }
          } ~
          (patch & pathEnd & entity(as[UpdateFullCategory])) { payload ⇒
            mutateOrFailures {
              CategoryManager.updateCategory(auth.model, categoryId, payload, context)
            }
          }
        } ~
        pathPrefix(Segment) { (context) ⇒
          (post & pathEnd & entity(as[CreateFullCategory])) { payload ⇒
            mutateOrFailures {
              CategoryManager.createCategory(auth.model, payload, context)
            }
          }
        } ~
        pathPrefix(IntNumber / "form") { categoryId ⇒
          (get & pathEnd) {
            getOrFailures {
              CategoryManager.getForm(categoryId)
            }
          }
        } ~
        pathPrefix(Segment / IntNumber / "baked") { (context, categoryId) ⇒
          (get & pathEnd) {
            getOrFailures {
              CategoryManager.getIlluminatedCategory(categoryId, context)
            }
          }
        } ~
        pathPrefix(Segment / IntNumber / "shadow") { (context, categoryId) ⇒
          (get & pathEnd) {
            getOrFailures {
              CategoryManager.getShadow(categoryId, context)
            }
          }
        }
      }
    }
  }
}
