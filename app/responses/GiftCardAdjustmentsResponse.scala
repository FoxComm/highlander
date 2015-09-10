package responses

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.{GiftCard, GiftCardAdjustment, GiftCardAdjustments, Order}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardAdjustmentsResponse {
  final case class Root(
    id: Int,
    amount: Int,
    availableBalance: Int,
    state: String,
    orderRef: String)

  def build(adjustment: GiftCardAdjustment, gc: GiftCard, order: Order): Root = {
    val amount = adjustment.getAmount
    Root(id = adjustment.id, amount = amount, availableBalance = gc.currentBalance + amount,
      state = adjustment.status.toString, orderRef = order.referenceNumber)
  }

  def forGiftCard(gc: GiftCard)(implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] = {
    (for {
      adjustments ← GiftCardAdjustments.filterByGiftCardId(gc.id)
      payments ← adjustments.payment
      orders ← payments.order
    } yield (adjustments, payments, orders)).result.run().map { results ⇒
      results.map { case (adjustment, payment, order) ⇒ build(adjustment, gc, order) }
    }.map(Xor.right)
  }
}

