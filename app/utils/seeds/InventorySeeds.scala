package utils.seeds

import models.inventory._
import models.inventory.Sku._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.generators.InventoryGenerator

import scala.concurrent.ExecutionContext.Implicits.global

trait InventorySeeds extends InventoryGenerator  {

  def createInventory: DbResultT[Unit] = for {
    _ ← * <~ Warehouses.createAll(warehouses)
    _ ← * <~ InventorySummaries.createAll(inventorySummaries)
  } yield {}

  def inventorySummaries: Seq[InventorySummary] = generateInventorySummaries(Seq(1, 2, 3, 4, 5, 6, 7))
}
