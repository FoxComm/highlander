package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.inventory._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.generators.InventoryGenerator

trait InventorySeeds extends InventoryGenerator {

  type Skus = (Sku, Sku, Sku, Sku, Sku, Sku, Sku)

  def createInventory: DbResultT[Skus] = for {
    skuIds ← * <~ Skus.createAllReturningIds(skus)
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
    _ ← * <~ generateInventories(skuIds, warehouseIds)
  } yield skuIds.seq.zip(skus).map { case (id, sku) ⇒ sku.copy(id = id) }.toList match {
      case s1 :: s2 :: s3 :: s4 :: s5 :: s6 :: s7 :: Nil ⇒ (s1, s2, s3, s4, s5, s6, s7)
      case other ⇒ ???
    }

  def skus: Seq[Sku] = Seq(
    Sku(code = "SKU-YAX", name = Some("Flonkey"), price = 3300),
    Sku(code = "SKU-BRO", name = Some("Bronkey"), price = 15300),
    Sku(code = "SKU-ABC", name = Some("Shark"), price = 4500),
    Sku(code = "SKU-SHH", name = Some("Sharkling"), price = 1500),
    Sku(code = "SKU-ZYA", name = Some("Dolphin"), price = 8800),
    Sku(code = "SKU-MRP", name = Some("Morphin"), price = 7700),
    // Why beetle? Cuz it's probably a bug. FIXME: add validation!!!
    Sku(code = "SKU-TRL", name = Some("Beetle"), price = -100, isHazardous = true, isActive = false))
}
