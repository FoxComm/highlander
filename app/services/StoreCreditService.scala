package services

import scala.concurrent.ExecutionContext

import models.{Customer, Customers, StoreCreditAdjustment, StoreAdmin, StoreCredit, StoreCreditManual,
StoreCreditManuals, StoreCredits}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import responses.StoreCredit.{Root, build}

object StoreCreditService {
  def createManual(admin: StoreAdmin, customerId: Int, payload: payloads.CreateManualStoreCredit)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    Customers.findById(customerId).flatMap {
      case Some(customer) ⇒
        val actions = for {
          origin  ← StoreCreditManuals.save(StoreCreditManual(adminId = admin.id, reasonId = payload.reasonId, subReasonId =
            payload.subReasonId))
          sc      ← StoreCredits.save(StoreCredit(customerId = customerId, originId = origin.id, originType = "CSR",
            currency = payload.currency, originalBalance = payload.amount))
          adjustments = List.empty[StoreCreditAdjustment]
        } yield (sc, origin, adjustments)

        actions.run().flatMap(res ⇒ Result.right((build _).tupled(res)))

      case None ⇒
        Result.left(NotFoundFailure(Customer, customerId).single)
    }
  }
}

