package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class GiftCardRefund(id: Int = 0, rmaId: Int) extends ModelWithIdParameter[GiftCardRefund]

object GiftCardRefund {}

class GiftCardRefunds(tag: Tag) extends GenericTable.TableWithId[GiftCardRefund](tag, "gift_card_refunds")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")

  def * = (id, rmaId) <> ((GiftCardRefund.apply _).tupled, GiftCardRefund.unapply)
}

object GiftCardRefunds extends TableQueryWithId[GiftCardRefund, GiftCardRefunds](
  idLens = GenLens[GiftCardRefund](_.id)
)(new GiftCardRefunds(_)){
}
