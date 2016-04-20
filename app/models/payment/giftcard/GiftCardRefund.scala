package models.payment.giftcard

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.db._

case class GiftCardRefund(id: Int = 0, rmaId: Int) extends FoxModel[GiftCardRefund]

object GiftCardRefund {}

class GiftCardRefunds(tag: Tag) extends FoxTable[GiftCardRefund](tag, "gift_card_refunds")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")

  def * = (id, rmaId) <> ((GiftCardRefund.apply _).tupled, GiftCardRefund.unapply)
}

object GiftCardRefunds extends FoxTableQuery[GiftCardRefund, GiftCardRefunds](
  idLens = GenLens[GiftCardRefund](_.id)
)(new GiftCardRefunds(_)){
}
