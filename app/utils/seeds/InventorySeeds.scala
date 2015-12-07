package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.inventory.{InventorySummaries, InventorySummary, Warehouse, Warehouses}
import models.{Sku, Skus}
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
    Sku(sku = "SKU-ABC", name = Some("Shark"), price = 4500),
    Sku(sku = "SKU-SHH", name = Some("Sharkling"), price = 1500),
    Sku(sku = "SKU-ZYA", name = Some("Dolphin"), price = 8800),
    Sku(sku = "SKU-MRP", name = Some("Morphin"), price = 7700),
    Sku(sku = "SKU-TRL", name = Some("Beetle"), price = -100, isHazardous = true)) // Why beetle? Cuz it's probably a bug

  def warehouse: Warehouse = Warehouse.buildDefault()

  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def inventorySummaries: Seq[InventorySummary] = Seq(
    InventorySummary.buildNew(warehouse.id, skuId = 1, onHand = 100),
    InventorySummary.buildNew(warehouse.id, skuId = 2, onHand = 100),
    InventorySummary.buildNew(warehouse.id, skuId = 3, onHand = 100))

}
