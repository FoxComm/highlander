package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Random.nextInt

import faker._
import models.product.SimpleProductData
import models.inventory.{Warehouses, Warehouse}
import models.inventory.summary.InventorySummary.AllSummaries
import models.inventory.summary._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def generateWarehouses = for {
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
  } yield warehouseIds

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

  def generateInventories(products: Seq[SimpleProductData], warehouseIds: Seq[Int]): DbResultT[Seq[AllSummaries]] = {
    val skuIds = products.map(_.skuId)
    DbResultT.sequence(for {
      skuId ←  skuIds
      warehouseId ← warehouseIds
    } yield generateInventory(skuId, warehouseId))
  }

  private def onHandRandom = nextInt(1000)
  private def onHoldRandom = nextInt(50)
  private def reservedRandom = nextInt(100)
}
