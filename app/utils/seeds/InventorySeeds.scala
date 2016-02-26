package utils.seeds

import models.inventory.Warehouses
import models.product.SimpleProductData
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.generators.InventoryGenerator

import scala.concurrent.ExecutionContext.Implicits.global

trait InventorySeeds extends InventoryGenerator  {

  def createInventory(products: Seq[SimpleProductData]): DbResultT[Unit] = for {
    warehouseIds ← * <~ Warehouses.createAllReturningIds(warehouses)
    _ ← * <~ generateInventories(products, warehouseIds)
  } yield {}

}
