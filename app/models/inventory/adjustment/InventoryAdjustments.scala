package models.inventory.adjustment

import java.time.Instant
import models.javaTimeSlickMapper
import models.inventory.{Skus, Warehouses}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class InventoryAdjustment(id: Int = 0, skuId: Int, warehouseId: Int, sellableId: Int, backorderId: Int,
  preorderId: Int, nonSellableId: Int, createdAt: Instant = Instant.now) extends ModelWithIdParameter[InventoryAdjustment]

  object InventoryAdjustment {

    sealed trait AdjustmentEvent {
      def skuId: Int
      def warehouseId: Int
      val name: String
    }

    final case class WmsOverride(skuId: Int, warehouseId: Int, onHand: Int, onHold: Int, reserved: Int,
      name: String = "WMS Sync") extends AdjustmentEvent

    final case class OrderPlaced(skuId: Int, warehouseId: Int, orderRef: String, quantity: Int,
      name: String = "Order placed") extends AdjustmentEvent

    final case class OrderPropagated(skuId: Int, warehouseId: Int, orderRef: String, quantity: Int,
      name: String = "Order propagated") extends AdjustmentEvent

    type AllAdjustments = (SellableInventoryAdjustment, PreorderInventoryAdjustment, BackorderInventoryAdjustment,
      NonSellableInventoryAdjustment) // TODO: replace with case class
  }

class InventoryAdjustments(tag: Tag) extends GenericTable.TableWithId[InventoryAdjustment](tag, "inventory_adjustments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def warehouseId = column[Int]("warehouse_id")
  def sellableId = column[Int]("sellable_id")
  def backorderId = column[Int]("backorder_id")
  def preorderId = column[Int]("preorder_id")
  def nonSellableId = column[Int]("nonsellable_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, skuId, warehouseId, sellableId, backorderId, preorderId, nonSellableId, createdAt) <>
    ((InventoryAdjustment.apply _).tupled, InventoryAdjustment.unapply)

  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def warehouse = foreignKey(Warehouses.tableName, skuId, Warehouses)(_.id)
  def sellable = foreignKey(SellableInventoryAdjustments.tableName, sellableId, SellableInventoryAdjustments)(_.id)
  def backorder = foreignKey(BackorderInventoryAdjustments.tableName, backorderId, BackorderInventoryAdjustments)(_.id)
  def preorder = foreignKey(PreorderInventoryAdjustments.tableName, preorderId, PreorderInventoryAdjustments)(_.id)
  def nonsellable = foreignKey(NonSellableInventoryAdjustments.tableName, nonSellableId, NonSellableInventoryAdjustments)(_.id)
}

object InventoryAdjustments extends TableQueryWithId[InventoryAdjustment, InventoryAdjustments](
  idLens = GenLens[InventoryAdjustment](_.id)
)(new InventoryAdjustments(_)) {

}
