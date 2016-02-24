package models.inventory

import java.time.Instant

import models.javaTimeSlickMapper
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class InventorySummary(
  id: Int, 
  warehouseId: Int,
  skuId: Int, 
  onHand: Int, 
  onHold: Int, 
  reserved: Int, 
  safetyStock: Option[Int] = None,
  skuType: SkuType = Sellable,
  updatedAt: Instant)
  extends ModelWithIdParameter[InventorySummary] {

  def availableForSale: Int = onHand - onHold - reserved - safetyStock.getOrElse(0)
}

class InventorySummaries(tag: Tag)
  extends GenericTable.TableWithId[InventorySummary](tag, "inventory_summaries")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def warehouseId = column[Int]("warehouse_id")
  def skuId = column[Int]("sku_id")
  def onHand = column[Int]("on_hand")
  def onHold = column[Int]("on_hold")
  def reserved = column[Int]("reserved")
  def safetyStock = column[Option[Int]]("safety_stock")
  def skuType = column[SkuType]("sku_type")
  def updatedAt = column[Instant]("updated_at")

  def * = (id, warehouseId, skuId, onHand, onHold, reserved, safetyStock, skuType, updatedAt) <>
  (( InventorySummary.apply _).tupled, InventorySummary.unapply)
}

object InventorySummary {

  def build(warehouseId: Int, skuId: Int, onHand: Int = 0, onHold: Int = 0, reserved: Int = 0, safetyStock:
  Option[Int] = None, skuType: SkuType = Sellable): InventorySummary = InventorySummary(
      id = 0,
      warehouseId = warehouseId,
      skuType = skuType,
      skuId = skuId,
      onHand = onHand,
      onHold = onHold,
      reserved = reserved,
      safetyStock = skuType match {
        case Sellable ⇒ safetyStock.fold(Some(0))(Some(_)) // Sellables should have safety stock
        case _        ⇒ None
      },
      updatedAt = Instant.now())
}

object InventorySummaries extends TableQueryWithId[InventorySummary, InventorySummaries](
  idLens = GenLens[InventorySummary](_.id)
)(new InventorySummaries(_)) {

  def findBySkuId(skuId: Int): QuerySeq =
    filter(_.skuId === skuId)

  def findBySkuIdInWarehouse(warehouseId: Int, id: Int): QuerySeq =
    filter(s ⇒ s.warehouseId === warehouseId && s.skuId === id)

  def findSellableBySkuId(skuId: Int): Query[(InventorySummaries, Warehouses), (InventorySummary, Warehouse), Seq] =
    for {
    warehouse ← Warehouses
    summary   ← filter(s ⇒ s.warehouseId === warehouse.id && s.skuId === skuId && s.skuType === (Sellable: SkuType))
  } yield (summary, warehouse)

  def findSellableBySkuIdInWarehouse(warehouseId: Int, skuId: Int): QuerySeq =
    findBySkuIdInWarehouse(warehouseId, skuId).filter(_.skuType === (Sellable: SkuType))
}
