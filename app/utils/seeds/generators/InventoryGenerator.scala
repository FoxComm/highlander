package utils.seeds.generators

import java.time.{Instant, ZoneId}

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Sku,Skus}
import models.inventory.{InventorySummary, InventorySummaries, Warehouse, Warehouses}
import utils.DbResultT._
import utils.DbResultT.implicits._
import GeneratorUtils.randomString
import scala.util.Random

import faker._;

trait InventoryGenerator {

  def warehouse: Warehouse = Warehouse.buildDefault()
  def warehouses: Seq[Warehouse] = Seq(warehouse)

  def generateInventorySummary(skuId: Int) = {
    Console.err.println(s"making inv sum ${skuId}")
    InventorySummary.buildNew(warehouse.id, skuId = skuId, onHand = Random.nextInt(100))
  }

  def generateInventorySummaries(skuIds: Seq[Int]) = 
    skuIds map generateInventorySummary

  def generateSku: Sku = {
    val base = new Base{}
    //TODO: Use a markov chain created from product descriptions to generate hilarious product names.
    Sku(
      sku = base.letterify("???-???"), 
      name = Some(Lorem.sentence(2)), 
      price = Random.nextInt(10000))
  }

  def generateInventory(skus: Seq[Sku])  = for {
    _ ← * <~ Warehouses.createAll(warehouses)
    skuIds ← * <~ Skus.createAllReturningIds(skus)
    _ ← * <~ InventorySummaries.createAll(generateInventorySummaries(skuIds))
  } yield skuIds

}
