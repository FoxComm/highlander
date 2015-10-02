package services

import models._
import responses.StoreCreditResponse

import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object CustomerCreditConverter {
  def toStoreCredit(code: String, customerId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[StoreCreditResponse.Root] = {

    val details = for {
      gc ← GiftCards.findByCode(code).one
      customer ← Customers._findById(customerId).extract.one
    } yield (gc, customer)

    db.run(details).flatMap {
      case (Some(gc), Some(customer)) ⇒
        if (gc.isActive) {
          val adj = for { adj <- GiftCardAdjustments.lastAuthByGiftCardId(gc.id).one } yield adj
          db.run(adj).flatMap {
            case Some(_) ⇒
              Result.failure(OpenTransactionsFailure)
            case _ ⇒
              val queries = (for {
                // Update status and make adjustment
                gcUpdated ← GiftCards.filter(_.id === gc.id).filter(_.status === (GiftCard.Active: GiftCard.Status))
                  .map(_.status).update(GiftCard.FullyRedeemed)
                adjustment ← GiftCards.redeemToStoreCredit(gc, admin)

                // Finally, convert to Store Credit
                conversion ← StoreCreditFromGiftCards.save(StoreCreditFromGiftCard(giftCardId = gc.id))
                sc ← StoreCredits.save(StoreCredit.buildFromGcTransfer(customerId, gc).copy(originId = conversion.id))
              } yield sc).transactionally

              Result.fromFuture(db.run(queries.map { sc ⇒ StoreCreditResponse.build(sc) }))
          }
        } else {
          Result.failure(GiftCardConvertFailure(gc))
        }
      case (None, _) ⇒
        Result.failure(GiftCardNotFoundFailure(code))
      case (_, None) ⇒
        Result.failure(NotFoundFailure(Customer, customerId))
    }
  }

  def toGiftCard(sc: StoreCredit, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[GiftCard] = {

    if (sc.isActive) {
      val giftCard = GiftCard(code = "x", originId = 0, originType = GiftCard.FromStoreCredit, currency = sc.currency,
        originalBalance = sc.currentBalance, currentBalance = sc.currentBalance)

      Result.fromFuture(db.run(for {
        conversion ← GiftCardFromStoreCredits.save(GiftCardFromStoreCredit(storeCreditId = sc.id))
        gc ← GiftCards.save(giftCard.copy(originId = conversion.id))
      } yield gc))
    } else {
      Result.failure(StoreCreditConvertFailure(sc))
    }
  }
}
