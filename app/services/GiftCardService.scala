package services

import scala.concurrent.ExecutionContext

import models.{StoreAdmins, GiftCards}
import responses.GiftCardResponse
import responses.GiftCardResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardService {
  def getCustomer(id: Int)(implicit db: Database, ec: ExecutionContext) = {
    for {
      storeAdmin ← StoreAdmins.findById(id)
    } yield storeAdmin
  }

  def getByCode(code: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    GiftCards.findByCode(code).one.run().flatMap {
      case Some(gc) ⇒ Result.right(GiftCardResponse.build(gc))
      case None ⇒ Result.left(NotFoundFailure(GiftCards, code).single)
    }
  }
}