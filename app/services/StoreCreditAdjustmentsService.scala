package services

import scala.concurrent.ExecutionContext

import responses.StoreCreditAdjustmentsResponse
import models.StoreCredits
import responses.StoreCreditAdjustmentsResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object StoreCreditAdjustmentsService {
  def forStoreCredit(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Seq[Root]] = {
    StoreCredits.findById(id).run().flatMap {
      case Some(storeCredit) ⇒
        StoreCreditAdjustmentsResponse.forStoreCredit(storeCredit)
      case _ ⇒
        Result.failure(StoreCreditNotFoundFailure(id))
    }
  }
}
