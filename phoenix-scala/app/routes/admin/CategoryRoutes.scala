package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.account.User
import payloads.CategoryPayloads._
import services.Authenticator.AuthData
import services.category.CategoryManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.JsonSupport._
import utils.json.codecs._

object CategoryRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

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
}
