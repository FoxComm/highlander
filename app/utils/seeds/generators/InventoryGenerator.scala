package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global

import models.inventory._
import utils.DbResultT._
import utils.DbResultT.implicits._
import scala.util.Random.nextInt

import scala.io.Source
import org.conbere.markov.MarkovChain
import faker._;

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  val stop = "\u0002"
  val start = "\u0003"
  val nameGenerator = Source.fromURL(getClass.getResource("/product_titles.txt"), "UTF-8").getLines
    .map(_.grouped(2)) //group characters in line into sets of 2
    .foldLeft(new MarkovChain[String](start, stop))((acc, wordChunks) =>
        acc.insert(wordChunks.map(_.toLowerCase).toList))

  def generateInventorySummary(skuId: Int): Seq[InventorySummary] =
    Seq(Backorder, NonSellable, Preorder, Sellable).map { typed ⇒
      InventorySummary.build(warehouseId = warehouse.id, skuId = skuId, skuType = typed, onHand = nextInt(500) + 50,
        onHold = nextInt(50), reserved = nextInt(100), safetyStock = Some(nextInt(20)))
    }

  def generateInventorySummaries(skuIds: Seq[Int]): Seq[InventorySummary] =
    skuIds.flatMap(generateInventorySummary)

  def generateSku: Sku = {
    val base = new Base{}
    //TODO: Use a markov chain created from product descriptions to generate hilarious product names.
    val sk = Sku(
      code = base.letterify("???-???"),
      name = Some(nameGenerator.generate(Math.max(5, nextInt(20))).mkString("")),
      price = nextInt(10000))

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
