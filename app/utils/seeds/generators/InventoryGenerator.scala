package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.inventory._
import models.product.SimpleProductData
import utils.DbResultT._
import utils.DbResultT.implicits._
import scala.util.Random.nextInt

import faker._;
import java.time.{Instant, ZoneId}
import org.conbere.markov.MarkovChain
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def generateInventorySummary(skuId: Int): Seq[InventorySummary] =
    Seq(Backorder, NonSellable, Preorder, Sellable).map { typed ⇒
      InventorySummary.build(warehouseId = warehouse.id, skuId = skuId, skuType = typed, onHand = nextInt(500) + 50,
        onHold = nextInt(50), reserved = nextInt(100), safetyStock = Some(nextInt(20)))
    }

  def generateInventorySummaries(skuIds: Seq[Int]): Seq[InventorySummary] =
    skuIds.flatMap(generateInventorySummary)

  def generateWarehouses = for {
    _ ← * <~ Warehouses.createAll(warehouses)
  } yield {}

  def generateInventory(products: Seq[SimpleProductData]) = for {
    skuIds ← * <~ products.map(_.skuId)
    _ ← * <~ InventorySummaries.createAll(generateInventorySummaries(skuIds))
  } yield skuIds

}
