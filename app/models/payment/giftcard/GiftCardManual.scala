package models.payment.giftcard

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.db._

case class GiftCardManual(id: Int = 0, adminId: Int, reasonId: Int) extends FoxModel[GiftCardManual]

object GiftCardManual {}

class GiftCardManuals(tag: Tag) extends FoxTable[GiftCardManual](tag, "gift_card_manuals")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def reasonId = column[Int]("reason_id")

  def * = (id, adminId, reasonId) <> ((GiftCardManual.apply _).tupled, GiftCardManual.unapply)
}

object GiftCardManuals extends FoxTableQuery[GiftCardManual, GiftCardManuals](
  idLens = GenLens[GiftCardManual](_.id)
  )(new GiftCardManuals(_)){
}
