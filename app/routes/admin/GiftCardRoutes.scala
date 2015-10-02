package routes.admin

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.immutable.Seq
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer

import cats.data.Xor
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import models._
import payloads._
import responses.{AllOrders, BulkOrderUpdateResponse, AdminNotes, FullOrder}
import services._
import slick.driver.PostgresDriver.api._
import utils.Apis
import utils.Slick
import utils.Slick.DbResult
import utils.Slick.implicits._
import Json4sSupport._
import utils.Http._

object GiftCardRoutes {

  def routes(implicit ec: ExecutionContext, db: Database,
    mat: Materializer, storeAdminAuth: AsyncAuthenticator[StoreAdmin], apis: Apis) = {

    authenticateBasicAsync(realm = "admin", storeAdminAuth) { admin ⇒

      pathPrefix("gift-cards") {
        (get & pathEnd) {
          complete {
            GiftCards.sortBy(_.id.desc).result.run().map(render(_))
          }
        } ~
        (patch & entity(as[payloads.GiftCardBulkUpdateStatusByCsr]) & pathEnd) { payload ⇒
          complete {
            GiftCardService.bulkUpdateStatusByCsr(payload, admin).map(renderGoodOrFailures)
          }
        } ~
        (get & path(Segment) & pathEnd) { code ⇒
          complete {
            GiftCardService.getByCode(code).map(renderGoodOrFailures)
          }
        } ~
        (get & path(Segment / "transactions") & pathEnd) { code ⇒
          complete {
            GiftCardAdjustmentsService.forGiftCard(code).map(renderGoodOrFailures)
          }
        } ~
        (post & path("_bulk") & entity(as[payloads.GiftCardBulkCreateByCsr]) & pathEnd) { payload ⇒
          complete {
            GiftCardService.createBulkByAdmin(admin, payload).map(renderGoodOrFailures)
          }
        } ~
        (post & entity(as[payloads.GiftCardCreateByCsr]) & pathEnd) { payload ⇒
          complete {
            GiftCardService.createByAdmin(admin, payload).map(renderGoodOrFailures)
          }
        } ~
        (patch & path(Segment) & entity(as[payloads.GiftCardUpdateStatusByCsr]) & pathEnd) { (code, payload) ⇒
          complete {
            GiftCardService.updateStatusByCsr(code, payload, admin).map(renderGoodOrFailures)
          }
        } ~
        path(Segment / "convert" / IntNumber) { (code, customerId) ⇒
          (post & pathEnd) {
            complete {
              CustomerCreditConverter.toStoreCredit(code, customerId, admin).map(renderGoodOrFailures)
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
            complete {
              NoteManager.deleteNote(noteId, admin).map(renderNothingOrFailures)
            }
          }
        }
      }
    }
  }
}
