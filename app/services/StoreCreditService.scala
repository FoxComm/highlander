package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.StoreCredit.Canceled
import models.{Reasons, Customer, Customers, StoreAdmin, StoreCredit, StoreCreditManual,
StoreCreditManuals, StoreCredits, StoreCreditAdjustments}
import responses.StoreCreditResponse
import responses.StoreCreditResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object StoreCreditService {
  type QuerySeq = Query[StoreCredits, StoreCredit, Seq]

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
        Result.failure(NotFoundFailure(StoreCredit, id))
    }
  }

  def updateStatusByCsr(id: Int, payload: payloads.StoreCreditUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {

    val finder = StoreCredits.filter(_.id === id)

    finder.findOneAndRun { sc ⇒
      sc.transitionTo(payload.status) match {
        case Xor.Left(message) ⇒ DbResult.failure(GeneralFailure(message))
        case Xor.Right(_) ⇒ (payload.status, payload.reason) match {
          case (Canceled, Some(reason)) ⇒
            cancelByCsr(finder, sc, payload)
          case (Canceled, None) ⇒
            DbResult.failure(EmptyCancellationReasonFailure)
          case (_, _) ⇒
            val update = finder.map(_.status).updateReturning(StoreCredits.map(identity), payload.status).head
            DbResult.fromDbio(update.flatMap { sc ⇒ DBIO.successful(StoreCreditResponse.build(sc)) })
        }
      }
    }
  }

  private def cancelByCsr(finder: QuerySeq, sc: StoreCredit, payload: payloads.StoreCreditUpdateStatusByCsr)
    (implicit ec: ExecutionContext, db: Database) = {

    StoreCreditAdjustments.lastAuthByStoreCreditId(sc.id).one.flatMap {
      case Some(adjustment) ⇒
        DbResult.failure(OpenTransactionsFailure)
      case None ⇒
        Reasons.findById(payload.reason.get).flatMap {
          case None ⇒
            DbResult.failure(InvalidCancellationReasonFailure)
          case _ ⇒
            val data = (payload.status, Some(sc.availableBalance), payload.reason)
            val cancellation = finder
              .map { gc ⇒ (gc.status, gc.canceledAmount, gc.canceledReason) }
              .updateReturning(StoreCredits.map(identity), data)
              .head

            DbResult.fromDbio(cancellation.flatMap { sc ⇒ DBIO.successful(StoreCreditResponse.build(sc)) })
        }
    }
  }

  private def fetchDetails(id: Int)(implicit db: Database, ec: ExecutionContext) = for {
    storeCredit ← StoreCredits.findById(id)
  } yield storeCredit
}

