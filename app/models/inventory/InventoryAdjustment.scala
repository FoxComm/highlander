package models.inventory

import java.time.Instant

import models.javaTimeSlickMapper
import models.order.Order
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class InventoryAdjustment(
  id: Int = 0, 
  warehouseId: Int,
  skuId: Int, 
  eventId: Int = 0,
  onHand: Int = 0, 
  onHold: Int = 0,
  reserved: Int = 0, 
  safetyStock: Option[Int] = None,
  skuType: SkuType = Sellable,
  noteId: Option[Int] = None,
  createdAt: Instant = Instant.now()) extends ModelWithIdParameter[InventoryAdjustment]

class InventoryAdjustments(tag: Tag) extends GenericTable.TableWithId[InventoryAdjustment](tag, "inventory_adjustments")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def warehouseId = column[Int]("warehouse_id")
  def skuId = column[Int]("sku_id")
  def eventId = column[Int]("event_id")
  def onHand = column[Int]("on_hand")
  def onHold = column[Int]("on_hold")
  def reserved = column[Int]("reserved")
  def safetyStock = column[Option[Int]]("safety_stock")
  def skuType = column[SkuType]("sku_type")
  def noteId = column[Option[Int]]("note_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, warehouseId, skuId, eventId, onHand, onHold, reserved, safetyStock, skuType, noteId, createdAt) <>
  ((InventoryAdjustment.apply _).tupled, InventoryAdjustment.unapply)
}

object InventoryAdjustments extends TableQueryWithId[InventoryAdjustment, InventoryAdjustments](
  idLens = GenLens[InventoryAdjustment](_.id)
)(new InventoryAdjustments(_)) {

  def createAdjustmentsForOrder(order: Order, warehouseId: Int): DBIO[Int] = {
    sqlu"""insert into inventory_adjustments (warehouse_id, event_id, sku_id, reserved, sku_type)
          select ${warehouseId} as warehouse_id, ${order.id} as order_id, oli_skus.sku_id as sku_id, count(*) as reserved, 'sellable' as sku_type from order_line_items as oli
          left join order_line_item_skus as oli_skus on origin_id = oli_skus.id
          where oli.order_id = ${order.id} and oli.origin_type = 'skuItem' group by sku_id"""
  }
}
