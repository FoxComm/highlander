package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random.nextInt

import models.inventory.adjustment.InventoryAdjustment.WmsOverride
import models.inventory.adjustment.SellableInventoryAdjustment
import models.product.SimpleProductData
import models.inventory.{Warehouse, Warehouses}
import models.inventory.summary.InventorySummary.AllSummaries
import models.inventory.summary._
import services.inventory.InventoryAdjustmentManager
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases.DB

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()

  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def generateWarehouses = for {
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
  } yield warehouseIds
}

trait InventorySummaryGenerator {
  import Rnd._

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
  import Rnd._

  def generateWmsAdjustment(skuId: Int, warehouseId: Int)(implicit db: DB): DbResultT[Seq[SellableInventoryAdjustment]] =
    DbResultT.sequence((1 to 10).map { _ ⇒
      val wmsOverride = WmsOverride(skuId, warehouseId, onHandRandom, onHoldRandom, reservedRandom)
      InventoryAdjustmentManager.wmsOverride(wmsOverride)
    })

  def generateWmsAdjustments(skuIds: Seq[Int], warehouseIds: Seq[Int])(implicit db: DB): DbResultT[Seq[SellableInventoryAdjustment]] =
    DbResultT.sequence(for {
      skuId ← skuIds
      warehouseId ← warehouseIds
    } yield generateWmsAdjustment(skuId, warehouseId)).map(_.flatten)
}

private object Rnd {
  def onHandRandom = nextInt(1000)
  def onHoldRandom = nextInt(50)
  def reservedRandom = nextInt(100)
}
