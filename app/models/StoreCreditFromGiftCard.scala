package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreCreditFromGiftCard(id: Int = 0, giftCardId: Int) extends ModelWithIdParameter[StoreCreditFromGiftCard]

object StoreCreditFromGiftCard

class StoreCreditFromGiftCards(tag: Tag) extends GenericTable.TableWithId[StoreCreditFromGiftCard](tag,
  "store_credit_from_gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")

  def * = (id, giftCardId) <> ((StoreCreditFromGiftCard.apply _).tupled, StoreCreditFromGiftCard.unapply)
}

object StoreCreditFromGiftCards extends TableQueryWithId[StoreCreditFromGiftCard, StoreCreditFromGiftCards](
  idLens = GenLens[StoreCreditFromGiftCard](_.id)
  )(new StoreCreditFromGiftCards(_)){
}
