package services

import scala.concurrent.ExecutionContext

import models.{Customers, GiftCards}
import responses.GiftCardResponse
import responses.GiftCardResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardService {
  def fetchDetails(code: String)(implicit db: Database, ec: ExecutionContext) = {
    for {
      giftCard ← GiftCards.findByCode(code).one.run()
      customer ← Customers.findById(1) // Mock Jacques
    } yield (giftCard, customer)
  }

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(code).flatMap {
      case (Some(giftCard), Some(customer)) ⇒
        val mockCustomer = customer.copy(password = null)
        Result.right(GiftCardResponse.build(giftCard, Some(mockCustomer)))
      case (Some(giftCard), None) ⇒
        Result.right(GiftCardResponse.build(giftCard))
      case (None, _) ⇒
        Result.left(NotFoundFailure(GiftCards, code).single)
    }
  }
}