package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import payloads._
import payloads.{AssignmentPayload, BulkAssignmentPayload}
import services.assignments.{GiftCardAssignmentsManager, GiftCardWatchersManager}
import services.giftcards._
import services.CustomerCreditConverter
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._

object GiftCardRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, admin: StoreAdmin, apis: Apis) = {

    activityContext(admin) { implicit ac ⇒
      pathPrefix("gift-cards") {
        (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
          goodOrFailures {
            GiftCardService.findAll
          }
        } ~
        (patch & pathEnd & entity(as[GiftCardBulkUpdateStateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.bulkUpdateStateByCsr(payload, admin)
          }
        } ~
        (post & pathEnd & entity(as[GiftCardBulkCreateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.createBulkByAdmin(admin, payload)
          }
        } ~
        (post & pathEnd & entity(as[GiftCardCreateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.createByAdmin(admin, payload)
          }
        } /* ~
        pathPrefix("assignees") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[GiftCardBulkAssignmentPayload]) { payload ⇒
              goodOrFailures {
                GiftCardAssignmentUpdater.assignBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[GiftCardBulkAssignmentPayload]) { payload ⇒
              goodOrFailures {
                GiftCardAssignmentUpdater.unassignBulk(admin, payload)
              }
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[GiftCardBulkWatchersPayload]) { payload ⇒
              goodOrFailures {
                GiftCardWatcherUpdater.watchBulk(admin, payload)
              }
            }
          } ~
          (post & path("delete") & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            entity(as[GiftCardBulkWatchersPayload]) { payload ⇒
              goodOrFailures {
                GiftCardWatcherUpdater.unwatchBulk(admin, payload)
              }
            }
          }
        } */
      } ~
      pathPrefix("gift-cards" / giftCardCodeRegex) { code ⇒
        (get & pathEnd) {
          goodOrFailures {
            GiftCardService.getByCode(code)
          }
        } ~
        (patch & pathEnd & entity(as[GiftCardUpdateStateByCsr])) { payload ⇒
          goodOrFailures {
            GiftCardService.updateStateByCsr(code, payload, admin)
          }
        } ~
        pathPrefix("assignees") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            nothingOrFailures {
              GiftCardAssignmentsManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            nothingOrFailures {
              GiftCardAssignmentsManager.unassign(code, assigneeId, admin)
            }
          }
        } ~
        pathPrefix("watchers") {
          (post & pathEnd & entity(as[AssignmentPayload])) { payload ⇒
            nothingOrFailures {
              GiftCardWatchersManager.assign(code, payload, admin)
            }
          } ~
          (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
            nothingOrFailures {
              GiftCardWatchersManager.unassign(code, assigneeId, admin)
            }
          }
        } ~
        path("transactions") {
          (get & pathEnd & sortAndPage) { implicit sortAndPage ⇒
            goodOrFailures {
              GiftCardAdjustmentsService.forGiftCard(code)
            }
          }
        } ~
        path("convert" / IntNumber) { customerId ⇒
          (post & pathEnd) {
            goodOrFailures {
              CustomerCreditConverter.toStoreCredit(code, customerId, admin)
            }
          }
        }
      }
    }
  }
}
