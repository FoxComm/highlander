package services

import scala.concurrent.ExecutionContext

import models.{Customers, GiftCards, StoreAdmins}
import responses.{GiftCardResponse, StoreAdminResponse}
import responses.GiftCardResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardService {
  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(code).flatMap {
      case (Some(giftCard), Some(createdBy)) ⇒
        Result.right(GiftCardResponse.build(giftCard, Some(StoreAdminResponse.build(createdBy))))
      case (Some(giftCard), _) ⇒
        Result.right(GiftCardResponse.build(giftCard))
      case _ ⇒
        Result.left(GiftCardNotFoundFailure(code).single)
    }
  }

  private def fetchDetails(code: String)(implicit db: Database, ec: ExecutionContext) = {
    for {
      giftCard ← GiftCards.findByCode(code).one.run()
      createdBy ← StoreAdmins.findById(1)
    } yield (giftCard, createdBy)
  }
}