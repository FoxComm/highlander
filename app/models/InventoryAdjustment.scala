package models

import utils.{TableQueryWithId, ModelWithIdParameter, RichTable, GenericTable}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}


case class InventoryAdjustment(id: Int, skuId: Int, inventoryEventId: Option[Int], availablePreOrder: Int, availableBackOrder: Int, outstandingPreOrders: Int, outstandingBackOrders: Int, description: Option[String], sourceNotes: Option[String]) extends ModelWithIdParameter {

}

// This is an exclusive arcs style sketchup.
class InventoryAdjustments(tag: Tag) extends GenericTable.TableWithId[InventoryAdjustment](tag, "inventory_adjustments") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def inventoryEventId = column[Int]("inventory_event_id")
  def skuId = column[Int]("sku_id")
  def reservedForFulfillment = column[Int]("reserved_for_fulfillment")
  def availableOnHand = column[Int]("available_on_hand")
  def availablePreOrder = column[Int]("available_pre_order")
  def availableBackOrder = column[Int]("available_back_order")
  def outstandingPreOrders =  column[Int]("outstanding_pre_orders") // How many have been preordered but not yet fulfilled
  def outstandingBackOrders = column[Int]("outstanding_back_orders") // How many unreconciled backorders are there.
  def description = column[Option[String]]("description")
  def sourceNotes = column[Option[String]]("source_notes") //Notes about a third party source


  def * = (id, skuId, inventoryEventId, availableBackOrder, availablePreOrder, availableBackOrder, outstandingPreOrders, outstandingBackOrders, description, sourceNotes) <> ((InventoryAdjustment.apply _).tupled, InventoryAdjustment.unapply)
}

object InventoryAdjustments extends TableQueryWithId[InventoryAdjustment, InventoryAdjustments](
  idLens = GenLens[InventoryAdjustment](_.id)
)(new InventoryAdjustments(_))