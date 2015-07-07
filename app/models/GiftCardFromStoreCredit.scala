package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.{GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId}

final case class GiftCardFromStoreCredit(id: Int = 0, giftCardId: Int) extends ModelWithIdParameter

object GiftCardFromStoreCredit {}

class GiftCardFromStoreCredits(tag: Tag) extends GenericTable.TableWithId[GiftCardFromStoreCredit](tag,
  "gift_card_from_store_credits") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")

  def * = (id, giftCardId) <> ((GiftCardFromStoreCredit.apply _).tupled, GiftCardFromStoreCredit.unapply)
}

object GiftCardFromStoreCredits extends TableQueryWithId[GiftCardFromStoreCredit, GiftCardFromStoreCredits](
  idLens = GenLens[GiftCardFromStoreCredit](_.id)
  )(new GiftCardFromStoreCredits(_)){
}
