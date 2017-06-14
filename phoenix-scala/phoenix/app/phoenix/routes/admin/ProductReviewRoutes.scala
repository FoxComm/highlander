package phoenix.routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import phoenix.models.account.User
import phoenix.payloads.ProductReviewPayloads.{CreateProductReviewByAdminPayload, UpdateProductReviewPayload}
import phoenix.services.Authenticator.AuthData
import phoenix.services.review.ProductReviewManager
import phoenix.utils.aliases._
import phoenix.utils.http.CustomDirectives._
import phoenix.utils.http.Http._
import phoenix.utils.http.JsonSupport._

object ProductReviewRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route =
    activityContext(auth) { implicit ac ⇒
      pathPrefix("review") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (get & path(IntNumber) & pathEnd) { reviewId ⇒
              getOrFailures {
                ProductReviewManager.getReview(reviewId)
              }
            } ~
            (post & entity(as[CreateProductReviewByAdminPayload]) & pathEnd) { payload ⇒
              mutateOrFailures {
                ProductReviewManager.createProductReview(payload.userId.getOrElse(auth.account.id), payload)
              }
            } ~
            (path(IntNumber) & patch & entity(as[UpdateProductReviewPayload]) & pathEnd) {
              (reviewId, payload) ⇒
                mutateOrFailures {
                  ProductReviewManager.updateProductReview(reviewId, payload)
                }
            } ~
            (delete & path(IntNumber) & pathEnd) { id ⇒
              deleteOrFailures {
                ProductReviewManager.archiveByContextAndId(id)
              }
            }
          }
        }
      }
    }
}
