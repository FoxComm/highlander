package models

import scala.concurrent.Future

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{TableQueryWithId, GenericTable, ModelWithIdParameter}
import utils.Slick.implicits._

final case class InventoryAdjustment(id: Int = 0, skuId: Int, inventoryEventId: Int, reservedForFulfillment: Int = 0, fulfilled: Int = 0,
                               availablePreOrder: Int = 0, availableBackOrder: Int = 0, outstandingPreOrders: Int = 0,
                               outstandingBackOrders: Int = 0, description: Option[String] = None,
                               sourceNotes: Option[String] = None) extends ModelWithIdParameter

class InventoryAdjustments(tag: Tag) extends GenericTable.TableWithId[InventoryAdjustment](tag, "inventory_adjustments")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def inventoryEventId = column[Int]("inventory_event_id")
  def reservedForFulfillment = column[Int]("reserved_for_fulfillment")
  def fulfilled = column[Int]("fulfilled")
  def availablePreOrder = column[Int]("available_pre_order")
  def availableBackOrder = column[Int]("available_back_order")
  def outstandingPreOrders =  column[Int]("outstanding_pre_orders") // How many have been preordered but not yet fulfilled
  def outstandingBackOrders = column[Int]("outstanding_back_orders") // How many unreconciled backorders are there.
  def description = column[Option[String]]("description")
  def sourceNotes = column[Option[String]]("source_notes") //Notes about a third party source

  def * = (id, skuId, inventoryEventId, reservedForFulfillment, fulfilled, availablePreOrder,
    availableBackOrder, outstandingPreOrders, outstandingBackOrders,
    description, sourceNotes) <> ((InventoryAdjustment.apply _).tupled, InventoryAdjustment.unapply)
}

object InventoryAdjustments extends TableQueryWithId[InventoryAdjustment, InventoryAdjustments](
  idLens = GenLens[InventoryAdjustment](_.id)
)(new InventoryAdjustments(_)) {

  def createAdjustmentsForOrder(order: Order)(implicit db: Database): Future[Int] =
    _createAdjustmentsForOrder(order).run()

  def _createAdjustmentsForOrder(order: Order): DBIO[Int] = {
    sqlu"""insert into inventory_adjustments (inventory_event_id, sku_id, reserved_for_fulfillment)
          select ${order.id} as order_id, sku_id, count(*) as n from order_line_items
          where order_id = ${order.id} group by sku_id"""
  }
}
