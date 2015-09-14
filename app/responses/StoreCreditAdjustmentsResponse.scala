package responses

import scala.concurrent.ExecutionContext

import models.{StoreCredit, StoreCreditAdjustment, StoreCreditAdjustments, Order}
import services.Result
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object StoreCreditAdjustmentsResponse {
  final case class Root(
    id: Int,
    debit: Int,
    state: StoreCreditAdjustment.Status,
    orderRef: String)

  def build(adjustment: StoreCreditAdjustment, sc: StoreCredit, order: Order): Root = {
    Root(id = adjustment.id, debit = adjustment.debit, state = adjustment.status, orderRef = order.referenceNumber)
  }

  def forStoreCredit(sc: StoreCredit)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    (for {
      adjustments ← StoreCreditAdjustments.filterByStoreCreditId(sc.id)
      payments ← adjustments.payment
      orders ← payments.order
    } yield (adjustments, payments, orders)).result.run().flatMap { results ⇒
      Result.good(results.map { case (adjustment, payment, order) ⇒ build(adjustment, sc, order) })
    }
  }
}

