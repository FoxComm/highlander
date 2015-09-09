package responses

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import models.{GiftCard, GiftCardAdjustment, GiftCardAdjustments}
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object GiftCardAdjustmentsResponse {
  final case class Root(id: Int, amount: Int, availableBalance: Int, orderId: Int, orderRef: String)

  def build(adjustment: GiftCardAdjustment): Root =
    Root(id = adjustment.id, amount = 0, availableBalance = 0, orderId = 0, orderRef = "")

  def forGiftCard(gc: GiftCard)(implicit ec: ExecutionContext, db: Database): Future[Nothing Xor Seq[Root]] = {
    (for {
      adjustments ← GiftCardAdjustments.filterByGiftCardId(gc.id)
    } yield adjustments).map { results ⇒
      results.map { case (adjustment) ⇒ build(adjustment) }
    }.map(Xor.right)
  }
}

