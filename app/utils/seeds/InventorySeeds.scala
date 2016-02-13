package utils.seeds

import models.inventory.{InventorySummaries, InventorySummary, Warehouse, Warehouses}
import models.product.{Sku, Skus}
import models.product.Sku._
import utils.DbResultT._
import utils.DbResultT.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

trait InventorySeeds {

  def createInventory: DbResultT[Unit] = for {
    _ ← * <~ Warehouses.createAll(warehouses)
    _ ← * <~ InventorySummaries.createAll(inventorySummaries)
  } yield {}

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def inventorySummaries: Seq[InventorySummary] = Seq(
    InventorySummary.buildNew(warehouse.id, skuId = 1, onHand = 373),
    InventorySummary.buildNew(warehouse.id, skuId = 2, onHand = 121),
    InventorySummary.buildNew(warehouse.id, skuId = 3, onHand = 45, onHold = 15),
    InventorySummary.buildNew(warehouse.id, skuId = 4, onHand = 57, onHold = 8),
    InventorySummary.buildNew(warehouse.id, skuId = 5, onHand = 89, reserved = 231),
    InventorySummary.buildNew(warehouse.id, skuId = 6, onHand = 92, onHold = 14, reserved = 35),
    InventorySummary.buildNew(warehouse.id, skuId = 7, onHand = -1))
}
