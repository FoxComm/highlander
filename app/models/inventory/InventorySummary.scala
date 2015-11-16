package models.inventory

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}
import models._
import java.time.Instant

final case class InventorySummary(
  id: Int, 
  warehouseId: Int,
  skuId: Int, 
  onHand: Int, 
  onHold: Int, 
  reserved: Int, 
  nonSellable: Int,
  safetyStock: Int,
  updatedAt: Instant)
  extends ModelWithIdParameter[InventorySummary]

class InventorySummaries(tag: Tag)
  extends GenericTable.TableWithId[InventorySummary](tag, "inventory_summaries")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def warehouseId = column[Int]("warehouse_id")
  def skuId = column[Int]("sku_id")
  def onHand = column[Int]("on_hand")
  def onHold = column[Int]("on_hold")
  def reserved = column[Int]("reserved")
  def nonSellable = column[Int]("non_sellable")
  def safetyStock = column[Int]("safety_stock")
  def updatedAt = column[Instant]("updated_at")

  def * = (id, warehouseId, skuId, onHand, onHold, reserved, nonSellable, safetyStock, updatedAt) <> 
  (( InventorySummary.apply _).tupled, InventorySummary.unapply)
}

object InventorySummary {
  def buildNew(warehouseId: Int, skuId: Int, onHand: Int = 0, reserved: Int = 0): InventorySummary =
    InventorySummary(
      id = 0,
      warehouseId = warehouseId,
      skuId = skuId,
      onHand = onHand,
      onHold = 0,
      reserved = reserved,
      nonSellable = 0,
      safetyStock = 0,
      updatedAt = Instant.now())
}

object InventorySummaries extends TableQueryWithId[InventorySummary, InventorySummaries](
  idLens = GenLens[InventorySummary](_.id)
)(new InventorySummaries(_)) {

  def findById(warehouseId: Int, id: Int): Query[InventorySummaries, InventorySummary, Seq] =
    filter(s => s.warehouseId === warehouseId && s.id === id)

  def findBySkuId(warehouseId: Int, id: Int): Query[InventorySummaries, InventorySummary, Seq] =
    filter(s => s.warehouseId === warehouseId && s.skuId === id)
}
