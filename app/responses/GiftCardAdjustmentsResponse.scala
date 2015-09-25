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
}

