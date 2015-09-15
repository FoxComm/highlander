package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.{Customer, Customers, StoreAdmin, StoreCredit, StoreCreditManual, StoreCreditManuals, StoreCredits}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import responses.StoreCreditResponse.{Root, build}

object StoreCreditService {
  def createManual(admin: StoreAdmin, customerId: Int, payload: payloads.CreateManualStoreCredit)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    Customers.findById(customerId).flatMap {
      case Some(customer) ⇒
        val actions = for {
          origin  ← StoreCreditManuals.save(StoreCreditManual(adminId = admin.id, reasonId = payload.reasonId,
            subReasonId = payload.subReasonId))
          sc      ← StoreCredits.save(StoreCredit(customerId = customerId, originId = origin.id, originType =
            StoreCredit.CsrAppeasement, currency = payload.currency, originalBalance = payload.amount))
        } yield sc

        actions.run().flatMap(sc ⇒ Result.right(build(sc)))

      case None ⇒
        Result.left(NotFoundFailure(Customer, customerId).single)
    }
  }

  def getById(id: Int)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    fetchDetails(id).run().flatMap {
      case Some(storeCredit) ⇒
        Result.right(responses.StoreCreditResponse.build(storeCredit))
      case _ ⇒
        Result.failure(StoreCreditNotFoundFailure(id))
    }
  }

  private def fetchDetails(id: Int)(implicit db: Database, ec: ExecutionContext) = for {
    storeCredit ← StoreCredits.findById(id)
  } yield storeCredit
}

