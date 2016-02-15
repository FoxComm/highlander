package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.inventory.Sku._
import models.inventory._
import utils.DbResultT._
import utils.DbResultT.implicits._

trait InventorySeeds {

  type Skus = (Sku, Sku, Sku, Sku, Sku, Sku, Sku)

  def createInventory: DbResultT[Skus] = for {
    skuIds ← * <~ Skus.createAllReturningIds(skus)
    _ ← * <~ Warehouses.createAll(warehouses)
    _ ← * <~ InventorySummaries.createAll(inventorySummaries)
  } yield skuIds.seq.zip(skus).map { case (id, sku) ⇒ sku.copy(id = id) }.toList match {
      case s1 :: s2 :: s3 :: s4 :: s5 :: s6 :: s7 :: Nil ⇒ (s1, s2, s3, s4, s5, s6, s7)
      case other ⇒ ???
    }

  def skus: Seq[Sku] = Seq(
    Sku(sku = "SKU-YAX", name = Some("Flonkey"), price = 3300),
    Sku(sku = "SKU-BRO", name = Some("Bronkey"), price = 15300),
    Sku(sku = "SKU-ABC", name = Some("Shark"), price = 4500, `type` = Preorder),
    Sku(sku = "SKU-SHH", name = Some("Sharkling"), price = 1500, `type` = Preorder),
    Sku(sku = "SKU-ZYA", name = Some("Dolphin"), price = 8800, `type` = Backorder),
    Sku(sku = "SKU-MRP", name = Some("Morphin"), price = 7700),
    // Why beetle? Cuz it's probably a bug. FIXME: add validation!!!
    Sku(sku = "SKU-TRL", name = Some("Beetle"), price = -100, isHazardous = true, `type` = NonSellable, isActive = false))

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
