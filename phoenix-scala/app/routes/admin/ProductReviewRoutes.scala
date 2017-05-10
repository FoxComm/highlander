package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import models.account.User
import payloads.ProductReviewPayloads.{CreateProductReviewPayload, UpdateProductReviewPayload}
import services.Authenticator.AuthData
import services.review.ProductReviewManager
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._
import utils.http.JsonSupport._

object ProductReviewRoutes {

  def routes(implicit ec: EC, db: DB, auth: AuthData[User]): Route = {

    activityContext(auth) { implicit ac ⇒
      pathPrefix("review") {
        pathPrefix(Segment) { contextName ⇒
          adminObjectContext(contextName) { implicit context ⇒
            (get & path(IntNumber) & pathEnd) { reviewId ⇒
              getOrFailures {
                ProductReviewManager.getReview(reviewId)
              }
            } ~
            (post & entity(as[CreateProductReviewPayload]) & pathEnd) { payload ⇒
              mutateOrFailures {
                ProductReviewManager.createProductReview(auth.account.id, payload)
              }
            } ~
            (path(IntNumber) & patch & entity(as[UpdateProductReviewPayload]) & pathEnd) {
              (reviewId, payload) ⇒
                mutateOrFailures {
                  ProductReviewManager.updateProductReview(auth.account.id, reviewId, payload)
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
}
