package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class GiftCardFromStoreCredit(id: Int = 0, storeCreditId: Int) extends ModelWithIdParameter[GiftCardFromStoreCredit]

object GiftCardFromStoreCredit

class GiftCardFromStoreCredits(tag: Tag) extends GenericTable.TableWithId[GiftCardFromStoreCredit](tag,
  "gift_card_from_store_credits")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeCreditId = column[Int]("store_credit_id")

  def * = (id, storeCreditId) <> ((GiftCardFromStoreCredit.apply _).tupled, GiftCardFromStoreCredit.unapply)
}

object GiftCardFromStoreCredits extends TableQueryWithId[GiftCardFromStoreCredit, GiftCardFromStoreCredits](
  idLens = GenLens[GiftCardFromStoreCredit](_.id)
  )(new GiftCardFromStoreCredits(_)){
}
