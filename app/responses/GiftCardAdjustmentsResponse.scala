package responses

import scala.concurrent.ExecutionContext

import models.{GiftCard, GiftCardAdjustment, GiftCardAdjustments, Order}
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
    (for {
      adjustments ← GiftCardAdjustments.filterByGiftCardId(gc.id)
      //payment ← adjustments.payment
      //orderRef ← payment.order.map(_.referenceNumber)
    } yield (adjustments/*, orderRef*/)).result.run().flatMap { results ⇒
      val response = results.map {
        case (adjustment/*, orderRef*/) ⇒ build(adjustment, gc, None)
      }

      Result.good(response)
    }
  }
}

