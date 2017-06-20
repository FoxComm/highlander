package phoenix.models.payment.giftcard

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class GiftCardManual(id: Int = 0, adminId: Int, reasonId: Int) extends FoxModel[GiftCardManual]

object GiftCardManual {}

class GiftCardManuals(tag: Tag) extends FoxTable[GiftCardManual](tag, "gift_card_manuals") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId  = column[Int]("admin_id")
  def reasonId = column[Int]("reason_id")

  def * = (id, adminId, reasonId) <> ((GiftCardManual.apply _).tupled, GiftCardManual.unapply)
}

object GiftCardManuals
    extends FoxTableQuery[GiftCardManual, GiftCardManuals](new GiftCardManuals(_))
    with ReturningId[GiftCardManual, GiftCardManuals] {
  val returningLens: Lens[GiftCardManual, Int] = lens[GiftCardManual].id
}
