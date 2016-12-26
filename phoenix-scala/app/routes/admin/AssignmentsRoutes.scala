package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.account.User
import models.cord.Cord.cordRefNumRegex
import models.inventory.Sku.skuCodeRegex
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import models.returns.Return.returnRefNumRegex
import payloads.AssignmentPayloads._
import services.assignments._
import services.Authenticator.AuthData
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object AssignmentsRoutes {

  def routes(implicit ec: EC,
             db: DB,
             auth: AuthData[User],
             tr: TracingRequest,
             trace: TracingExtensionImpl) = {

    activityContext(auth.model) { implicit ac ⇒
      // Customers Bulk Assignments
      pathPrefix("customers") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Customers Single Assignments
      pathPrefix("customers" / IntNumber) { accountId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              CustomerAssignmentsManager.list(accountId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CustomerAssignmentsManager.assign(accountId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CustomerAssignmentsManager.unassign(accountId, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              CustomerWatchersManager.list(accountId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CustomerWatchersManager.assign(accountId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CustomerWatchersManager.unassign(accountId, assigneeId, auth.model)
            }
          }
        }
      } ~
      // Gift Cards Bulk Assignments
      pathPrefix("gift-cards") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Gift Cards Single Assignments
      pathPrefix("gift-cards" / giftCardCodeRegex) { code ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              GiftCardAssignmentsManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              GiftCardAssignmentsManager.assign(code, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              GiftCardAssignmentsManager.unassign(code, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              GiftCardWatchersManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              GiftCardWatchersManager.assign(code, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              GiftCardWatchersManager.unassign(code, assigneeId, auth.model)
            }
          }
        }
      } ~
      // Orders Bulk Assignments
      pathPrefix("orders") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Orders Single Assignments
      pathPrefix("orders" / cordRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              OrderAssignmentsManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              OrderAssignmentsManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              OrderAssignmentsManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              OrderWatchersManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              OrderWatchersManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              OrderWatchersManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        }
      } ~
      // Carts Bulk Assignments
      pathPrefix("carts") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                CartAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                CartAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                CartWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                CartWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Carts Single Assignments
      pathPrefix("carts" / cordRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              CartAssignmentsManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CartAssignmentsManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CartAssignmentsManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              CartWatchersManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CartWatchersManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CartWatchersManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        }
      } ~
      // Returns Bulk Assignments
      pathPrefix("returns") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Returns Single Assignments
      pathPrefix("returns" / returnRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              ReturnAssignmentsManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              ReturnAssignmentsManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ReturnAssignmentsManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              ReturnWatchersManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              ReturnWatchersManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ReturnWatchersManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        }
      } ~
      // Products Bulk Assignments
      pathPrefix("products") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Products Single Assignments
      pathPrefix("products" / IntNumber) { productId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              ProductAssignmentsManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              ProductAssignmentsManager.assign(productId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ProductAssignmentsManager.unassign(productId, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              ProductWatchersManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              ProductWatchersManager.assign(productId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ProductWatchersManager.unassign(productId, assigneeId, auth.model)
            }
          }
        }
      } ~
      // SKUs Bulk Assignments
      pathPrefix("skus") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // SKUs Single Assignments
      pathPrefix("skus" / skuCodeRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              SkuAssignmentsManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              SkuAssignmentsManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              SkuAssignmentsManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              SkuWatchersManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              SkuWatchersManager.assign(refNum, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              SkuWatchersManager.unassign(refNum, assigneeId, auth.model)
            }
          }
        }
      } ~
      // Promotions Bulk Assignments
      pathPrefix("promotions") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Promotions Single Assignments
      pathPrefix("promotions" / IntNumber) { promotionId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              PromotionAssignmentsManager.list(promotionId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              PromotionAssignmentsManager.assign(promotionId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              PromotionAssignmentsManager.unassign(promotionId, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              PromotionWatchersManager.list(promotionId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              PromotionWatchersManager.assign(promotionId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              PromotionWatchersManager.unassign(promotionId, assigneeId, auth.model)
            }
          }
        }
      } ~
      // Coupons Bulk Assignments
      pathPrefix("coupons") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponAssignmentsManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponAssignmentsManager.unassignBulk(auth.model, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponWatchersManager.assignBulk(auth.model, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponWatchersManager.unassignBulk(auth.model, payload)
              }
            }
          }
        }
      } ~
      // Coupons Single Assignments
      pathPrefix("coupons" / IntNumber) { couponId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              CouponAssignmentsManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CouponAssignmentsManager.assign(couponId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CouponAssignmentsManager.unassign(couponId, assigneeId, auth.model)
            }
          }
        } ~
        pathPrefix("watchers") {
          (get & pathEnd) {
            getOrFailures {
              CouponWatchersManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CouponWatchersManager.assign(couponId, payload, auth.model)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CouponWatchersManager.unassign(couponId, assigneeId, auth.model)
            }
          }
        }
      }
    }
  }
}
