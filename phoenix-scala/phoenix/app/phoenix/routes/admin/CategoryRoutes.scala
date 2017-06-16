package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import phoenix.models.account.User
import phoenix.payloads.CategoryPayloads._
import phoenix.services.Authenticator.AuthData
import phoenix.services.category.CategoryManager
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object CategoryRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User], apis: Apis): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("categories") {
        pathPrefix(IntNumber / "form") { categoryId ⇒
          (get & pathEnd) {
            getOrFailures {
              CategoryManager.getForm(categoryId)
            }
          }
        } ~
        pathPrefix(Segment) { context ⇒
          pathPrefix(IntNumber) { categoryId ⇒
            (get & pathEnd) {
              getOrFailures {
                CategoryManager.getCategory(categoryId, context)
              }
            } ~
            (patch & pathEnd & entity(as[UpdateFullCategory])) { payload ⇒
              mutateOrFailures {
                CategoryManager.updateCategory(auth.model, categoryId, payload, context)
              }
            } ~
            pathPrefix("baked") {
              (get & pathEnd) {
                getOrFailures {
                  CategoryManager.getIlluminatedCategory(categoryId, context)
                }
              }
            } ~
            pathPrefix("shadow") {
              (get & pathEnd) {
                getOrFailures {
                  CategoryManager.getShadow(categoryId, context)
                }
              }
            }
          } ~
          (post & pathEnd & entity(as[CreateFullCategory])) { payload ⇒
            mutateOrFailures {
              CategoryManager.createCategory(auth.model, payload, context)
            }
          }
        }
      }
    }
}
