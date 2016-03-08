package utils.seeds

import models.inventory.Warehouses
import models.product.SimpleProductData
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.generators._

import scala.concurrent.ExecutionContext.Implicits.global

trait InventorySeeds extends InventoryGenerator with InventorySummaryGenerator {

  def createInventory(products: Seq[SimpleProductData]): DbResultT[Unit] = for {
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
    _ ← * <~ generateInventories(products, warehouseIds)
  } yield {}

}
