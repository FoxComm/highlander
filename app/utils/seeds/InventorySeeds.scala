package utils.seeds

import scala.concurrent.ExecutionContext.Implicits.global

import models.inventory.Warehouses
import models.product.SimpleProductData
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._
import utils.seeds.generators._

trait InventorySeeds
    extends InventoryGenerator
    with InventorySummaryGenerator
    with InventoryAdjustmentsGenerator {

  def createInventory(products: Seq[SimpleProductData])(implicit db: DB): DbResultT[Unit] =
    for {
      warehouseIds ← * <~ Warehouses.map(_.id).result.toXor
      skuIds = products.map(_.skuId)
      _ ← * <~ generateInventoriesForSkus(skuIds, warehouseIds)
      _ ← * <~ generateWmsAdjustmentsSeq(skuIds, warehouseIds)
    } yield {}
}
