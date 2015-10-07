package routes.admin

import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import models._

import responses.AdminNotes
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Http._
import utils.Slick.implicits._
import utils.CustomDirectives._

import scala.concurrent.ExecutionContext

object GiftCardRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("gift-cards") {
        (get & pathEnd) {
          good {
            GiftCards.sortBy(_.id.desc).result.run()
          }
        } ~
        (patch & entity(as[payloads.GiftCardBulkUpdateStatusByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            GiftCardService.bulkUpdateStatusByCsr(payload, admin)
          }
        } ~
        (get & path(Segment) & pathEnd) { code ⇒
          goodOrFailures {
            GiftCardService.getByCode(code)
          }
        } ~
        (get & path(Segment / "transactions") & pathEnd) { code ⇒
          goodOrFailures {
            GiftCardAdjustmentsService.forGiftCard(code)
          }
        } ~
        (post & path("_bulk") & entity(as[payloads.GiftCardBulkCreateByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            GiftCardService.createBulkByAdmin(admin, payload)
          }
        } ~
        (post & entity(as[payloads.GiftCardCreateByCsr]) & pathEnd) { payload ⇒
          goodOrFailures {
            GiftCardService.createByAdmin(admin, payload)
          }
        } ~
        (patch & path(Segment) & entity(as[payloads.GiftCardUpdateStatusByCsr]) & pathEnd) { (code, payload) ⇒
          goodOrFailures {
            GiftCardService.updateStatusByCsr(code, payload, admin)
          }
        } ~
        path(Segment / "convert" / IntNumber) { (code, customerId) ⇒
          (post & pathEnd) {
            goodOrFailures {
              CustomerCreditConverter.toStoreCredit(code, customerId, admin)
            }
          }
        } ~
        path(Segment / "notes") { code ⇒
          (get & pathEnd) {
            complete {
              whenFound(GiftCards.findByCode(code).one.run()) { giftCard ⇒ AdminNotes.forGiftCard(giftCard) }
            }
          } ~
          (post & entity(as[payloads.CreateNote]) & pathEnd) { payload ⇒
            complete {
              whenFound(GiftCards.findByCode(code).one.run()) { giftCard ⇒
                NoteManager.createGiftCardNote(giftCard, admin, payload)
              }
            }
          }
        } ~
        path(Segment / "notes" / IntNumber) { (code, noteId) ⇒
          (patch & entity(as[payloads.UpdateNote]) & pathEnd) { payload ⇒
            complete {
              whenFound(GiftCards.findByCode(code).one.run()) { _ ⇒
                NoteManager.updateNote(noteId, admin, payload)
              }
            }
          } ~
          (delete & pathEnd) {
            nothingOrFailures {
              NoteManager.deleteNote(noteId, admin)
            }
          }
        }
      }
    }
  }
}
