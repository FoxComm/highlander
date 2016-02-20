package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._

import models.StoreAdmin
import models.payment.giftcard.GiftCard.giftCardCodeRegex
import payloads._
import services.giftcards._
import services.CustomerCreditConverter
import services.Authenticator.{AsyncAuthenticator}
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.CustomDirectives._
import utils.Http._
import utils.aliases._

import models.auth.Session.requireAdminAuth

object GiftCardRoutes {

  def routes(implicit ec: EC, db: DB, mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    requireAdminAuth { admin ⇒
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
          } ~
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
          }
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
            (post & pathEnd & entity(as[GiftCardAssignmentPayload])) { payload ⇒
              goodOrFailures {
                GiftCardAssignmentUpdater.assign(admin, code, payload.assignees)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
              goodOrFailures {
                GiftCardAssignmentUpdater.unassign(admin, code, assigneeId)
              }
            }
          } ~
          pathPrefix("watchers") {
            (post & pathEnd & entity(as[GiftCardWatchersPayload])) { payload ⇒
              goodOrFailures {
                GiftCardWatcherUpdater.watch(admin, code, payload.watchers)
              }
            } ~
            (delete & path(IntNumber) & pathEnd) { assigneeId ⇒
              goodOrFailures {
                GiftCardWatcherUpdater.unwatch(admin, code, assigneeId)
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
}
