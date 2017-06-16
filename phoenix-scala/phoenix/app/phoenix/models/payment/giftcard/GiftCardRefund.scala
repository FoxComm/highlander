package phoenix.models.payment.giftcard

import shapeless._
import slick.jdbc.PostgresProfile.api._
import core.db._

case class GiftCardRefund(id: Int = 0, returnId: Int) extends FoxModel[GiftCardRefund]

object GiftCardRefund {}

class GiftCardRefunds(tag: Tag) extends FoxTable[GiftCardRefund](tag, "gift_card_refunds") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId = column[Int]("return_id")

  def * = (id, returnId) <> ((GiftCardRefund.apply _).tupled, GiftCardRefund.unapply)
}

object GiftCardRefunds
    extends FoxTableQuery[GiftCardRefund, GiftCardRefunds](new GiftCardRefunds(_))
    with ReturningId[GiftCardRefund, GiftCardRefunds] {
  val returningLens: Lens[GiftCardRefund, Int] = lens[GiftCardRefund].id
}
