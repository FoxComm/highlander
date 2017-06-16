package phoenix.models.payment.storecredit

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class StoreCreditFromGiftCard(id: Int = 0, giftCardId: Int) extends FoxModel[StoreCreditFromGiftCard]

object StoreCreditFromGiftCard

class StoreCreditFromGiftCards(tag: Tag)
    extends FoxTable[StoreCreditFromGiftCard](tag, "store_credit_from_gift_cards") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")

  def * =
    (id, giftCardId) <> ((StoreCreditFromGiftCard.apply _).tupled, StoreCreditFromGiftCard.unapply)
}

object StoreCreditFromGiftCards
    extends FoxTableQuery[StoreCreditFromGiftCard, StoreCreditFromGiftCards](new StoreCreditFromGiftCards(_))
    with ReturningId[StoreCreditFromGiftCard, StoreCreditFromGiftCards] {
  val returningLens: Lens[StoreCreditFromGiftCard, Int] = lens[StoreCreditFromGiftCard].id
}
