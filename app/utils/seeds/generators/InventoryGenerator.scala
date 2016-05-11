package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random.nextInt

import models.inventory.InventoryAdjustment.WmsOverride
import models.product.SimpleProductData
import models.inventory._
import models.inventory.summary.InventorySummary.AllSummaries
import models.inventory.summary._
import services.inventory.InventoryAdjustmentManager
import utils.aliases.DB
import utils.db._
import utils.db.DbResultT._
import Rnd._

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()

  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def generateWarehouses = for {
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
  } yield warehouseIds
}

trait InventorySummaryGenerator {

  def generateInventory(skuId: Int, warehouseId: Int): DbResultT[AllSummaries] = for {
    sellable ← * <~ SellableInventorySummaries.create(SellableInventorySummary(onHand = onHandRandom, onHold =
      onHoldRandom, reserved = reservedRandom, safetyStock = nextInt(20)))
    preorder ← * <~ PreorderInventorySummaries.create(PreorderInventorySummary(onHand = onHandRandom, onHold =
      onHoldRandom, reserved = reservedRandom))
    backorder ← * <~ BackorderInventorySummaries.create(BackorderInventorySummary(onHand = onHandRandom, onHold =
      onHoldRandom, reserved = reservedRandom))
    nonsellable ← * <~ NonSellableInventorySummaries.create(NonSellableInventorySummary(onHand = onHandRandom, onHold =
      onHoldRandom, reserved = reservedRandom))
    summary ← * <~ InventorySummaries.create(InventorySummary(skuId = skuId, warehouseId = warehouseId, sellableId =
      sellable.id, preorderId = preorder.id, backorderId = backorder.id, nonSellableId = nonsellable.id))
  } yield (sellable, preorder, backorder, nonsellable)

  def generateInventories(products: Seq[SimpleProductData], warehouseIds: Seq[Int]): DbResultT[Seq[AllSummaries]] =
    generateInventoriesForSkus(products.map(_.skuId), warehouseIds)

  def generateInventoriesForSkus(skuIds: Seq[Int], warehouseIds: Seq[Int]): DbResultT[Seq[AllSummaries]] =
    DbResultT.sequence(for {
      skuId ← skuIds
      warehouseId ← warehouseIds
    } yield generateInventory(skuId, warehouseId))
}

trait InventoryAdjustmentsGenerator {

  def generateWmsAdjustments(skuId: Int, warehouseId: Int)(implicit db: DB): DbResultT[Unit] =
    DbResultT.sequence((1 to nextInt(10)).map { _ ⇒
      val wmsOverride = WmsOverride(skuId, warehouseId, onHandRandom / 2, onHoldRandom / 2, reservedRandom / 2)
      InventoryAdjustmentManager.applyWmsAdjustment(wmsOverride)
    }).map(_ ⇒ Unit)

  def generateWmsAdjustmentsSeq(skuIds: Seq[Int], warehouseIds: Seq[Int])(implicit db: DB): DbResultT[Unit] =
    DbResultT.sequence(for {
      skuId ← skuIds
      warehouseId ← warehouseIds
    } yield generateWmsAdjustments(skuId, warehouseId)).map(_ ⇒ Unit)
}

private object Rnd {
  def onHandRandom = nextInt(10000) + nextInt(5000) + 2500
  def onHoldRandom = nextInt(50) + 25
  def reservedRandom = nextInt(100) + 50
}
