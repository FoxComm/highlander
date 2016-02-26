package utils.seeds.generators

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Random.nextInt

import faker._
import models.inventory._
import models.inventory.summary.InventorySummary.AllSummaries
import models.inventory.summary._
import org.conbere.markov.MarkovChain
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  val stop = "\u0002"
  val start = "\u0003"
  val nameGenerator = Source.fromURL(getClass.getResource("/product_titles.txt"), "UTF-8").getLines
    .map(_.grouped(2)) //group characters in line into sets of 2
    .foldLeft(new MarkovChain[String](start, stop))((acc, wordChunks) =>
        acc.insert(wordChunks.map(_.toLowerCase).toList))

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

  def generateInventories(skuIds: Seq[Int], warehouseIds: Seq[Int]): DbResultT[Seq[AllSummaries]] =
    DbResultT.sequence(for {
      skuId ← skuIds
      warehouseId ← warehouseIds
    } yield generateInventory(skuId, warehouseId))

  private def onHandRandom = nextInt(1000)
  private def onHoldRandom = nextInt(50)
  private def reservedRandom = nextInt(100)
}
