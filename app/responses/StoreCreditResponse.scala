package responses

import org.joda.time.DateTime
import models.{StoreCreditAdjustment, StoreCreditAdjustments}
import scala.concurrent.{ExecutionContext, Future}
import slick.driver.PostgresDriver.api._
import utils.Money.Currency

object StoreCreditResponse {
  type Response = Future[Root]
  
  final case class Root(
    id: Int,
    originId: Int,
    originType: String,
    currency: Currency,
    originalBalance: Int,
    currentBalance: Int,
    availableBalance: Int,
    status: models.StoreCredit.Status,
    createdAt: DateTime,
    canceledReason: Option[String] = None,
    adjustments: Seq[DisplayAdjustment])

  final case class DisplayAdjustment(
    id: Int,
    debit: Int,
    status: StoreCreditAdjustment.Status
    )

  def fromStoreCredit(storeCredit: models.StoreCredit)(implicit ec: ExecutionContext, db: Database): Response = {
    db.run(fetchStoreCreditDetails(storeCredit).result).map { case (adjustments) ⇒
      build(
        storeCredit = storeCredit,
        adjustments = adjustments
      )
    }
  }

  def build(storeCredit: models.StoreCredit, adjustments: Seq[StoreCreditAdjustment] = Seq.empty): Root = {
    Root(id = storeCredit.id,
      originId = storeCredit.originId,
      originType = storeCredit.originType,
      currency = storeCredit.currency,
      originalBalance = storeCredit.originalBalance,
      currentBalance = storeCredit.currentBalance,
      availableBalance = storeCredit.availableBalance,
      status = storeCredit.status,
      createdAt = storeCredit.createdAt,
      canceledReason = storeCredit.canceledReason,
      adjustments = adjustments.map {
        adj ⇒ DisplayAdjustment(id = adj.id,
          debit = adj.debit,
          status = adj.status)}
    )
  }

  private def fetchStoreCreditDetails(storeCredit: models.StoreCredit)(implicit ec: ExecutionContext) = {
    for {
      adjustments ← StoreCreditAdjustments.filter(_.storeCreditId === storeCredit.id)
    } yield adjustments
  }


}
