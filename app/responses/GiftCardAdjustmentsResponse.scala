package responses

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.{GiftCard, GiftCardAdjustment, GiftCardAdjustments}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardAdjustmentsResponse {
  final case class Root(
    id: Int,
    amount: Int,
    availableBalance: Int,
    state: String,
    orderRef: String)

  def build(adjustment: GiftCardAdjustment, gc: GiftCard): Root = {
    val amount = (adjustment.credit, adjustment.debit) match {
      case (credit, 0) ⇒ credit
      case (0, debit) ⇒ -debit
    }

    Root(id = adjustment.id, amount = amount, availableBalance = gc.availableBalance + amount,
      state = adjustment.status.toString, orderRef = "")
  }


  def forGiftCard(gc: GiftCard)(implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] = {
    (for {
      adjustments ← GiftCardAdjustments.filterByGiftCardId(gc.id)
    } yield adjustments).map { results ⇒
      results.map { case (adjustment) ⇒ build(adjustment, gc) }
    }.map(Xor.right)
  }
}

