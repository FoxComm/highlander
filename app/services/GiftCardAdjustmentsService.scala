package services

import scala.concurrent.ExecutionContext

import responses.GiftCardAdjustmentsResponse
import models.GiftCards
import responses.GiftCardAdjustmentsResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardAdjustmentsService {
  def forGiftCard(code: String)(implicit db: Database, ec: ExecutionContext): Result[Seq[Root]] = {
    GiftCards.findByCode(code).one.run().flatMap {
      case Some(giftCard) ⇒
        GiftCardAdjustmentsResponse.forGiftCard(giftCard)
      case _ ⇒
        Result.failure(GiftCardNotFoundFailure(code))
    }
  }
}
