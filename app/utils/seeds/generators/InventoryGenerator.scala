package utils.seeds.generators


import GeneratorUtils.randomString
import models.inventory.{InventorySummary, InventorySummaries, Warehouse, Warehouses}
import models.product.{Sku, Skus, SimpleProductData}
import scala.util.Random
import utils.DbResultT._
import utils.DbResultT.implicits._

import faker._;
import java.time.{Instant, ZoneId}
import org.conbere.markov.MarkovChain
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def generateInventorySummary(skuId: Int) = {
    InventorySummary.buildNew(warehouse.id, skuId = skuId, onHand = Random.nextInt(100))
  }

  def generateInventorySummaries(skuIds: Seq[Int]) = 
    skuIds map generateInventorySummary

  def generateWarehouses = for {
    _ ← * <~ Warehouses.createAll(warehouses)
  } yield {}

  def generateInventory(products: Seq[SimpleProductData]) = for {
    skuIds ← * <~ products.map(_.skuId)
    _ ← * <~ InventorySummaries.createAll(generateInventorySummaries(skuIds))
  } yield skuIds

}
