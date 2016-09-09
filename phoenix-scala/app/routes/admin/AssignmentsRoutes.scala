package routes.admin

import akka.http.scaladsl.server.Directives._

import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.cord.Cord.cordRefNumRegex
import models.inventory.Sku.skuCodeRegex
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import models.returns.Return.returnRefNumRegex
import payloads.AssignmentPayloads._
import services.assignments._
import utils.aliases._
import utils.http.CustomDirectives._
import utils.http.Http._

object AssignmentsRoutes {

  def routes(implicit ec: EC, db: DB, admin: StoreAdmin) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("customers") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CustomerWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { accountId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              CustomerAssignmentsManager.list(accountId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CustomerAssignmentsManager.assign(accountId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CustomerAssignmentsManager.unassign(accountId, assigneeId, admin)
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
              CustomerWatchersManager.assign(accountId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CustomerWatchersManager.unassign(accountId, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("gift-cards") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                GiftCardWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("gift-cards" / giftCardCodeRegex) { code ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              GiftCardAssignmentsManager.list(code)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              GiftCardAssignmentsManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              GiftCardAssignmentsManager.unassign(code, assigneeId, admin)
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
              GiftCardWatchersManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              GiftCardWatchersManager.unassign(code, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("orders") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                OrderWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("orders" / cordRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              OrderAssignmentsManager.list(refNum)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              OrderAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              OrderAssignmentsManager.unassign(refNum, assigneeId, admin)
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
              OrderWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              OrderWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("returns") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                ReturnWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("returns" / returnRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              ReturnAssignmentsManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              ReturnAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ReturnAssignmentsManager.unassign(refNum, assigneeId, admin)
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
              ReturnWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ReturnWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("products") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                ProductWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("products" / IntNumber) { productId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              ProductAssignmentsManager.list(productId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              ProductAssignmentsManager.assign(productId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ProductAssignmentsManager.unassign(productId, assigneeId, admin)
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
              ProductWatchersManager.assign(productId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              ProductWatchersManager.unassign(productId, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("skus") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              mutateOrFailures {
                SkuWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("skus" / skuCodeRegex) { refNum ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              SkuAssignmentsManager.list(refNum)
            }
          } ~
          (post & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              SkuAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              SkuAssignmentsManager.unassign(refNum, assigneeId, admin)
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
              SkuWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              SkuWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("promotions") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                PromotionWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("promotions" / IntNumber) { promotionId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              PromotionAssignmentsManager.list(promotionId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              PromotionAssignmentsManager.assign(promotionId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              PromotionAssignmentsManager.unassign(promotionId, assigneeId, admin)
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
              PromotionWatchersManager.assign(promotionId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              PromotionWatchersManager.unassign(promotionId, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("coupons") {
        pathPrefix("assignees") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd) {
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              mutateOrFailures {
                CouponWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("coupons" / IntNumber) { couponId ⇒
        pathPrefix("assignees") {
          (get & pathEnd) {
            getOrFailures {
              CouponAssignmentsManager.list(couponId)
            }
          } ~
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            mutateOrFailures {
              CouponAssignmentsManager.assign(couponId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CouponAssignmentsManager.unassign(couponId, assigneeId, admin)
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
              CouponWatchersManager.assign(couponId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            mutateOrFailures {
              CouponWatchersManager.unassign(couponId, assigneeId, admin)
            }
          }
        }
      }
    }
  }
}
