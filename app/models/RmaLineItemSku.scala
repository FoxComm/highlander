package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaLineItemSku(id: Int = 0, rmaId: Int, orderLineItemSkuId: Int) extends ModelWithIdParameter

object RmaLineItemSku {}

class RmaLineItemSkus(tag: Tag) extends
GenericTable.TableWithId[RmaLineItemSku](tag, "rma_line_item_gift_skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rmaId")
  def orderLineItemSkuId = column[Int]("order_line_item_sku_id")

  def * = (id, rmaId, orderLineItemSkuId) <> ((RmaLineItemSku.apply _).tupled, RmaLineItemSku.unapply)
}

object RmaLineItemSkus extends TableQueryWithId[RmaLineItemSku, RmaLineItemSkus](
  idLens = GenLens[RmaLineItemSku](_.id)
)(new RmaLineItemSkus(_)){

}
