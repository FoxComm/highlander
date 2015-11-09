package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaLineItemGiftCard(id: Int = 0, rmaId: Int, orderLineItemGiftCardId: Int,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[RmaLineItemGiftCard]

object RmaLineItemGiftCard {}

class RmaLineItemGiftCards(tag: Tag) extends
GenericTable.TableWithId[RmaLineItemGiftCard](tag, "rma_line_item_gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def orderLineItemGiftCardId = column[Int]("order_line_item_gift_card_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, orderLineItemGiftCardId,
    createdAt) <> ((RmaLineItemGiftCard.apply _).tupled, RmaLineItemGiftCard.unapply)
}

object RmaLineItemGiftCards extends TableQueryWithId[RmaLineItemGiftCard, RmaLineItemGiftCards](
  idLens = GenLens[RmaLineItemGiftCard](_.id)
)(new RmaLineItemGiftCards(_)){
  
}
