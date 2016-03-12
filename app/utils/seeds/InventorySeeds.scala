package utils.seeds

import models.inventory.Warehouses
import models.product.SimpleProductData
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.generators._
import scala.concurrent.ExecutionContext.Implicits.global

import utils.aliases._

trait InventorySeeds extends InventoryGenerator with InventorySummaryGenerator with InventoryAdjustmentsGenerator {

  def createInventory(products: Seq[SimpleProductData])(implicit db: DB): DbResultT[Unit] = for {
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
    skuIds = products.map(_.skuId)
    _ ← * <~ generateInventoriesForSkus(skuIds, warehouseIds)
    _ ← * <~ generateWmsAdjustmentsSeq(skuIds, warehouseIds)
  } yield {}

}
