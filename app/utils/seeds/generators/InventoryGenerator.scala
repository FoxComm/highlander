package utils.seeds.generators


import GeneratorUtils.randomString
import models.inventory.{InventorySummary, InventorySummaries, Warehouse, Warehouses}
import models.product.{Sku, Skus}
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

  val stop = "\u0002"
  val start = "\u0003"
  def nameGenerator = Source.fromURL(getClass.getResource("/product_titles.txt"), "UTF-8").getLines
    .map(_.grouped(2)) //group characters in line into sets of 2
    .foldLeft(new MarkovChain[String](start, stop))((acc, wordChunks) => 
        acc.insert(wordChunks.map(_.toLowerCase).toList))

  def generateInventorySummary(skuId: Int) = {
    InventorySummary.buildNew(warehouse.id, skuId = skuId, onHand = Random.nextInt(100))
  }

  def generateInventorySummaries(skuIds: Seq[Int]) = 
    skuIds map generateInventorySummary

  def generateSku: Sku = {
    val base = new Base{}
    //TODO: Use a markov chain created from product descriptions to generate hilarious product names.
    val sk = Sku(
      sku = base.letterify("???-???"), 
      name = Some(nameGenerator.generate(Math.max(5, Random.nextInt(20))).mkString("")), 
      price = Random.nextInt(10000))

    Console.err.println(s"sku: ${sk.name}")
    sk
  }

  def generateWarehouses = for {
    _ ← * <~ Warehouses.createAll(warehouses)
  } yield {}

  def generateInventory(skus: Seq[Sku])  = for {
    skuIds ← * <~ Skus.createAllReturningIds(skus)
    _ ← * <~ InventorySummaries.createAll(generateInventorySummaries(skuIds))
  } yield skuIds

}
