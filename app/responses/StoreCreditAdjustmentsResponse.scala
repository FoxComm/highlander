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
    availableBalance: Int,
    state: StoreCreditAdjustment.Status,
    orderRef: String)

  def build(adj: StoreCreditAdjustment, orderRef: String): Root = {
    Root(id = adj.id, debit = adj.debit, availableBalance = adj.availableBalance, state = adj.status,
      orderRef = orderRef)
  }

  def forStoreCredit(sc: StoreCredit)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    (for {
      adjustments ← StoreCreditAdjustments.filterByStoreCreditId(sc.id)
      payments ← adjustments.payment
      orderRef ← payments.order.map(_.referenceNumber)
    } yield (adjustments, orderRef)).result.run().flatMap { results ⇒
      Result.good(results.map { case (adjustment, orderRef) ⇒ build(adjustment, orderRef) })
    }
  }
}

