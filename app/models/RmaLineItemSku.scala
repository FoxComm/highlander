package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaLineItemSku(id: Int = 0, rmaId: Int, orderLineItemSkuId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[RmaLineItemSku]

object RmaLineItemSku {}

class RmaLineItemSkus(tag: Tag) extends GenericTable.TableWithId[RmaLineItemSku](tag, "rma_line_item_skus") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def orderLineItemSkuId = column[Int]("order_line_item_sku_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, orderLineItemSkuId, createdAt) <> ((RmaLineItemSku.apply _).tupled, RmaLineItemSku.unapply)
}

object RmaLineItemSkus extends TableQueryWithId[RmaLineItemSku, RmaLineItemSkus](
  idLens = GenLens[RmaLineItemSku](_.id)
)(new RmaLineItemSkus(_)){

}
