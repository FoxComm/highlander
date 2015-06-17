package models

import utils.{RichTable, GenericTable}


class InventoryAdjustment {

}

class InventoryAdjustments(tag: Tag) extends GenericTable.TableWithId[InventoryAdjustment](tag, "inventory_adjustments") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def order_id = column[Option[Int]]("order_id")
  def purchase_order_receipt_id = column[Option[Int]]("purchase_order_receipt_id")
  def rma_receipt_id = column[Option[Int]]("")
  def cycle_count_id = column[Option[Int]]("order_id")
  def physical_inventory_event_id = column[Option[Int]]("order_id")
  def onHandAdjustment = column[Int]("available_on_hand")
  def availablePreOrder = column[Int]("available_pre_order")
  def availableBackOrder = column[Int]("available_pre_order")
  def outstandingPreOrders =  column[Int]("available_pre_order") // How many have been preordered but not yet fulfilled
  def outstandingBackOrders = column[Int]("available_pre_order") // How many unreconciled backorders are there.
  def description = column[Option[String]]("description")
  def sourceNotes = column[Option[String]]("source_notes") //Notes about a third party source
}
