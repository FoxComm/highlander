package services

import models._
import responses._

import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._

object CustomerCreditConverter {
  def toStoreCredit(code: String, customerId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[StoreCreditResponse.Root] = {

    val details = for {
      gc ← GiftCards.findByCode(code).one
      customer ← Customers.findById(customerId).extract.one
      adj ← gc match {
        case Some(giftCard) ⇒ GiftCardAdjustments.lastAuthByGiftCardId(giftCard.id).one
        case _              ⇒ DBIO.successful(None)
      }
    } yield (gc, customer, adj)

    db.run(details.transactionally).flatMap {
      case (Some(gc), Some(customer), None) if gc.isActive ⇒
        val queries = (for {
          // Update status and make adjustment
          gcUpdated ← GiftCards.findActiveByCode(gc.code).map(_.status).update(GiftCard.FullyRedeemed)
          adjustment ← GiftCards.redeemToStoreCredit(gc, admin)

          // Finally, convert to Store Credit
          conversion ← StoreCreditFromGiftCards.save(StoreCreditFromGiftCard(giftCardId = gc.id))
          sc ← StoreCredits.save(StoreCredit.buildFromGcTransfer(customerId, gc).copy(originId = conversion.id))
        } yield sc).transactionally

        Result.fromFuture(db.run(queries.map { sc ⇒ StoreCreditResponse.build(sc) }))
      case (Some(gc), _, _) if !gc.isActive ⇒
        Result.failure(GiftCardConvertFailure(gc))
      case (_, _, Some(_)) ⇒
        Result.failure(OpenTransactionsFailure)
      case (None, _, _) ⇒
        Result.failure(GiftCardNotFoundFailure(code))
      case (_, None, _) ⇒
        Result.failure(NotFoundFailure(Customer, customerId))
    }
  }

  def toGiftCard(id: Int, customerId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[GiftCardResponse.Root] = {

    val details = for {
      sc ← StoreCredits.findActiveById(id).one
      customer ← Customers.findById(customerId).extract.one
      adj ← sc match {
        case Some(storeCredit)  ⇒ StoreCreditAdjustments.lastAuthByStoreCreditId(storeCredit.id).one
        case _                  ⇒ DBIO.successful(None)
      }
    } yield (sc, customer, adj)

    db.run(details.transactionally).flatMap {
      case (Some(sc), Some(customer), None) if sc.isActive ⇒
        val giftCard = GiftCard(originId = 0, originType = GiftCard.FromStoreCredit, currency = sc.currency,
          originalBalance = sc.currentBalance, currentBalance = sc.currentBalance)

        val queries = (for {
          // Update status and make adjustment
          scUpdated ← StoreCredits.findActiveById(sc.id).map(_.status).update(StoreCredit.FullyRedeemed)
          adjustment ← StoreCredits.redeemToGiftCard(sc, admin)

          // Convert to Gift Card
          conversion ← GiftCardFromStoreCredits.save(GiftCardFromStoreCredit(storeCreditId = sc.id))
          gc ← GiftCards.save(giftCard.copy(originId = conversion.id))
        } yield gc).transactionally

        val adminResponse = StoreAdminResponse.build(admin)
        Result.fromFuture(db.run(queries.map { gc ⇒ GiftCardResponse.build(gc, None, Some(adminResponse)) }))
      case (Some(sc), _, _) if !sc.isActive ⇒
        Result.failure(StoreCreditConvertFailure(sc))
      case (_, _, Some(_)) ⇒
        Result.failure(OpenTransactionsFailure)
      case (None, _, _) ⇒
        Result.failure(NotFoundFailure(StoreCredit, id))
      case (_, None, _) ⇒
        Result.failure(NotFoundFailure(Customer, customerId))
    }
  }
}
