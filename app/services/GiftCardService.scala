package services

import scala.concurrent.ExecutionContext

import models.{StoreAdmins, GiftCards}
import responses.GiftCardResponse
import responses.GiftCardResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardService {
  def fetchDetails(code: String)(implicit db: Database, ec: ExecutionContext) = {
    for {
      giftCard ← GiftCards.findByCode(code).one.run()
      storeAdmin ← StoreAdmins.findById(1) // Mock
    } yield (giftCard, storeAdmin)
  }

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(code).flatMap {
      case (Some(giftCard), storeAdmin)       ⇒ Result.right(GiftCardResponse.build(giftCard, storeAdmin))
      case (None, _)                          ⇒ Result.left(NotFoundFailure(GiftCards, code).single)
    }
  }
}