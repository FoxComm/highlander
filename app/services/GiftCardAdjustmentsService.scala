package services

import scala.concurrent.ExecutionContext

import models.{Customers, GiftCards, GiftCardAdjustment, GiftCardAdjustments}
import responses.GiftCardAdjustmentsResponse
import responses.GiftCardAdjustmentsResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardAdjustmentsService {
  def fetchGiftCard(code: String)(implicit db: Database, ec: ExecutionContext) = {
    for {
      giftCard ← GiftCards.findByCode(code).one.run()
    } yield giftCard
  }

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchGiftCard(code).flatMap {
      case Some(giftCard) ⇒
        //Result.right(GiftCardAdjustmentsResponse.forGiftCard(giftCard))
        Result.left(GiftCardNotFoundFailure(code).single)
      case None ⇒
        Result.left(GiftCardNotFoundFailure(code).single)
    }
  }
}