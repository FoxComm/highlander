package responses

import scala.concurrent.ExecutionContext

import models.{GiftCards, StoreCredit, StoreCreditAdjustment, StoreCreditAdjustments, Orders, OrderPayments}
import services.Result
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object StoreCreditAdjustmentsResponse {
  final case class Root(
    id: Int,
    debit: Int,
    state: StoreCreditAdjustment.Status,
    orderRef: Option[String])

  def build(adjustment: StoreCreditAdjustment, orderRef: Option[String] = None): Root = {
    Root(id = adjustment.id, debit = adjustment.debit, state = adjustment.status, orderRef = orderRef)
  }

  def forStoreCredit(sc: StoreCredit)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    val query = StoreCreditAdjustments.filterByStoreCreditId(sc.id)
      .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
      .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

    db.run(query.result).map { results ⇒
      results.map {
        case ((adj, Some(payment)), Some(order)) ⇒ build(adj, Some(order.referenceNumber))
        case ((adj, _), _)                       ⇒ build(adj)
      }
    }.flatMap(Result.good)
  }
}

