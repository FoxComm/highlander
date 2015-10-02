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

    val finder = GiftCards.findByCode(code)

    finder.findOneAndRun { gc ⇒
      if (gc.isActive) {
        val storeCredit = StoreCredit(customerId = customerId, originId = 0,
          originType = StoreCredit.GiftCardTransfer, currency = gc.currency, originalBalance = gc.currentBalance,
          currentBalance = gc.currentBalance)

        GiftCardAdjustments.lastAuthByGiftCardId(gc.id).one.flatMap {
          case Some(adj) ⇒
            DbResult.failure(OpenTransactionsFailure)
          case _ ⇒
            val queries = (for {
              // Update status and make adjustment
              gcUpdated ← finder.map(_.status).update(GiftCard.FullyRedeemed)
              adjustment ← GiftCards.redeemToStoreCredit(gc, admin)

              // Finally, convert to Store Credit
              conversion ← StoreCreditFromGiftCards.save(StoreCreditFromGiftCard(giftCardId = gc.id))
              sc ← StoreCredits.save(storeCredit.copy(originId = conversion.id))
            } yield sc).transactionally

            DbResult.fromDbio(queries.map { sc ⇒ StoreCreditResponse.build(sc) } )
        }
      } else {
        DbResult.failure(GiftCardConvertFailure(gc))
      }
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
