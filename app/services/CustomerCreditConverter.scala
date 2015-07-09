package services

import models._

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

object CustomerCreditConverter {
  def toStoreCredit(gc: GiftCard, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[StoreCredit Or Failure] = {

    if (gc.isActive) {
      val storeCredit = StoreCredit(customerId = customerId, originId = 0, originType = "storeCreditFromGiftCard",
        currency = gc.currency, originalBalance = gc.currentBalance, currentBalance = gc.currentBalance)

      db.run(for {
        conversion ← StoreCreditFromGiftCards.save(StoreCreditFromGiftCard(giftCardId = gc.id))
        sc ← StoreCredits.save(storeCredit.copy(originId = conversion.id))
      } yield sc).map(Good(_))
    } else {
      Future.successful(Bad(GeneralFailure(s"cannot convert a gift card with status '${gc.status}'")))
    }
  }

  def toGiftCard(sc: StoreCredit, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[GiftCard Or Failure] = {

    if (sc.isActive) {
      val giftCard = GiftCard(code = "x", originId = 0, originType =
        "giftCardFromStoreCredit", currency = sc.currency,
        originalBalance = sc.currentBalance, currentBalance = sc.currentBalance)

      db.run(for {
        conversion ← GiftCardFromStoreCredits.save(GiftCardFromStoreCredit(storeCreditId = sc.id))
        gc ← GiftCards.save(giftCard.copy(originId = conversion.id))
      } yield gc).map(Good(_))
    } else {
      Future.successful(Bad(GeneralFailure(s"cannot convert a store credit with status '${sc.status}'")))
    }
  }
}
