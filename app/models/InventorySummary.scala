package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class InventorySummary(id: Int, skuId: Int, availableOnHand: Int, availablePreOrder: Int, availableBackOrder: Int,
                            outstandingPreOrders: Int, outstandingBackOrders: Int) extends ModelWithIdParameter

class InventorySummaries(tag: Tag)
  extends GenericTable.TableWithId[InventorySummary](tag, "inventory_summaries")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def availableOnHand = column[Int]("available_on_hand")
  def availablePreOrder = column[Int]("available_pre_order")
  def availableBackOrder = column[Int]("available_back_order")
  def outstandingPreOrders =  column[Int]("outstanding_pre_orders") // How many have been preordered but not yet fulfilled
  def outstandingBackOrders = column[Int]("outstanding_back_orders") // How many unreconciled backorders are there.

  def * = (id, skuId, availableOnHand, availablePreOrder, availableBackOrder,
    outstandingPreOrders, outstandingBackOrders) <> (( InventorySummary.apply _).tupled, InventorySummary.unapply)
}

object InventorySummary {
  def buildNew(skuId: Int, availableOnHand: Int): InventorySummary =
    InventorySummary(
      id = 0,
      skuId = skuId,
      availableOnHand = availableOnHand,
      availablePreOrder = 0,
      availableBackOrder = 0,
      outstandingBackOrders = 0,
      outstandingPreOrders = 0)
}

object InventorySummaries extends TableQueryWithId[InventorySummary, InventorySummaries](
  idLens = GenLens[InventorySummary](_.id)
)(new InventorySummaries(_)) {

  def findBySkuId(id: Int): Query[InventorySummaries, InventorySummary, Seq] =
    filter(_.skuId === id)
}
