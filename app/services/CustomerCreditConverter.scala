package services

import models._

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

object CustomerCreditConverter {
  def toStoreCredit(gc: GiftCard, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[StoreCredit Or Failure] = {

    gc.status match {
      case GiftCard.Canceled ⇒
        Future.successful(Bad(GeneralFailure("cannot convert a canceled gift card")))
      case GiftCard.Hold ⇒
        Future.successful(Bad(GeneralFailure("cannot convert an on hold gift card")))
      case _ ⇒
        val storeCredit = StoreCredit(customerId = customerId, originId = 0, originType = "storeCreditFromGiftCard",
        currency = gc.currency, originalBalance = gc.currentBalance, currentBalance = gc.currentBalance)

        db.run(for {
          conversion ← StoreCreditFromGiftCards.save(StoreCreditFromGiftCard(giftCardId = gc.id))
          sc ← StoreCredits.save(storeCredit.copy(originId = conversion.id))
        } yield sc).map(Good(_))
    }
  }

  def toGiftCard(sc: StoreCredit, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[GiftCard Or Failure] = {

    sc.status match {
      case StoreCredit.Canceled ⇒
        Future.successful(Bad(GeneralFailure("cannot convert a canceled store credit")))
      case StoreCredit.Hold ⇒
        Future.successful(Bad(GeneralFailure("cannot convert an on hold store credit")))
      case _ ⇒
        val giftCard = GiftCard(customerId = Some(customerId), code = "x", originId = 0, originType =
          "giftCardFromStoreCredit", currency = sc.currency,
          originalBalance = sc.currentBalance, currentBalance = sc.currentBalance)

        db.run(for {
          conversion ← GiftCardFromStoreCredits.save(GiftCardFromStoreCredit(storeCreditId = sc.id))
          gc ← GiftCards.save(giftCard.copy(originId = conversion.id))
        } yield gc).map(Good(_))
    }
  }
}
