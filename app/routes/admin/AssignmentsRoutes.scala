package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models.StoreAdmin
import models.order.Order.orderRefNumRegex
import models.rma.Rma.rmaRefNumRegex
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import services.assignments._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.Slick.implicits._
import utils.aliases._

object AssignmentsRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("customers") {
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[Int]]) { payload ⇒
              goodOrFailures {
                CustomerWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("customers" / IntNumber) { customerId ⇒
        pathPrefix("assignees") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              CustomerAssignmentsManager.assign(customerId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              CustomerAssignmentsManager.unassign(customerId, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              CustomerWatchersManager.assign(customerId, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              CustomerWatchersManager.unassign(customerId, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("gift-cards") {
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                GiftCardWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("gift-cards" / giftCardCodeRegex) { code ⇒
        pathPrefix("assignees") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              GiftCardAssignmentsManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              GiftCardAssignmentsManager.unassign(code, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              GiftCardWatchersManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              GiftCardWatchersManager.unassign(code, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("orders") {
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                OrderWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("orders"/ orderRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              OrderAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              OrderAssignmentsManager.unassign(refNum, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              OrderWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              OrderWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      } ~
      pathPrefix("rmas") {
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                RmaAssignmentsManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                RmaAssignmentsManager.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                RmaWatchersManager.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[BulkAssignmentPayload[String]]) { payload ⇒
              goodOrFailures {
                RmaWatchersManager.unassignBulk(admin, payload)
              }
            }
          }
        }
      } ~
      pathPrefix("rmas"/ rmaRefNumRegex) { refNum ⇒
        pathPrefix("assignees") {
          (post & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              RmaAssignmentsManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              RmaAssignmentsManager.unassign(refNum, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & entity(as[AssignmentPayload])) { payload ⇒
            goodOrFailures {
              RmaWatchersManager.assign(refNum, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            goodOrFailures {
              RmaWatchersManager.unassign(refNum, assigneeId, admin)
            }
          }
        }
      }      
    }
  }
}
