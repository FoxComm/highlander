package responses

import scala.concurrent.{Future, ExecutionContext}

import cats.data.Xor
import models.{Orders, GiftCard, GiftCardAdjustment, GiftCardAdjustments, Order, OrderPayments}
import services.Result
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardAdjustmentsResponse {
  final case class Root(
    id: Int,
    amount: Int,
    availableBalance: Int,
    state: GiftCardAdjustment.Status,
    orderRef: Option[String])

  def build(adjustment: GiftCardAdjustment, gc: GiftCard, orderRef: Option[String] = None): Root = {
    val amount = adjustment.getAmount
    Root(id = adjustment.id, amount = amount, availableBalance = gc.currentBalance + amount,
      state = adjustment.status, orderRef = orderRef)
  }

  def forGiftCard(gc: GiftCard)(implicit ec: ExecutionContext, db: Database): Result[Seq[Root]] = {
    val query = GiftCardAdjustments.filterByGiftCardId(gc.id)
      .joinLeft(OrderPayments).on(_.orderPaymentId === _.id)
      .joinLeft(Orders).on(_._2.map(_.orderId) === _.id)

    db.run(query.result).map { results ⇒
      results.map {
        case ((adj, Some(payment)), Some(order)) ⇒ build(adj, gc, Some(order.referenceNumber))
        case ((adj, _), _)                       ⇒ build(adj, gc)
      }
    }.flatMap(Result.good)
  }
}

