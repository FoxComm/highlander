package models.inventory.summary

import java.time.Instant

import models.inventory.{Skus, Warehouses}
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db._

case class InventorySummary(id: Int = 0, skuId: Int, warehouseId: Int, sellableId: Int, backorderId: Int,
  preorderId: Int, nonSellableId: Int, createdAt: Instant = Instant.now) extends FoxModel[InventorySummary]

object InventorySummary {
  type AllSummaries = (SellableInventorySummary,PreorderInventorySummary, BackorderInventorySummary,
    NonSellableInventorySummary) // TODO: replace with case class
}

class InventorySummaries(tag: Tag) extends FoxTable[InventorySummary](tag, "inventory_summaries") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuId = column[Int]("sku_id")
  def warehouseId = column[Int]("warehouse_id")
  def sellableId = column[Int]("sellable_id")
  def backorderId = column[Int]("backorder_id")
  def preorderId = column[Int]("preorder_id")
  def nonSellableId = column[Int]("nonsellable_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, skuId, warehouseId, sellableId, backorderId, preorderId, nonSellableId, createdAt) <>
    ((InventorySummary.apply _).tupled, InventorySummary.unapply)

  def sku = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def warehouse = foreignKey(Warehouses.tableName, skuId, Warehouses)(_.id)
  def sellable = foreignKey(SellableInventorySummaries.tableName, sellableId, SellableInventorySummaries)(_.id)
  def backorder = foreignKey(BackorderInventorySummaries.tableName, backorderId, BackorderInventorySummaries)(_.id)
  def preorder = foreignKey(PreorderInventorySummaries.tableName, preorderId, PreorderInventorySummaries)(_.id)
  def nonsellable = foreignKey(NonSellableInventorySummaries.tableName, nonSellableId, NonSellableInventorySummaries)(_.id)
}

object InventorySummaries extends FoxTableQuery[InventorySummary, InventorySummaries](
  idLens = lens[InventorySummary].id
)(new InventorySummaries(_)) {

  def findSellableBySkuId(skuId: Int) = for {
    warehouse ← Warehouses
    summary   ← filter(s ⇒ s.skuId === skuId && s.warehouseId === warehouse.id)
    sellable  ← SellableInventorySummaries.filter(_.id === summary.sellableId)
  } yield (sellable, warehouse)

  // https://github.com/slick/slick/issues/1316
  def findBySkuIdInWarehouse(skuId: Int, warehouseId: Int) = for {
    ((((summary, sellable), preorder), backorder), nonsellable) ← InventorySummaries
      .join(SellableInventorySummaries).on(_.sellableId === _.id)
      .join(PreorderInventorySummaries).on(_._1.preorderId === _.id)
      .join(BackorderInventorySummaries).on(_._1._1.backorderId === _.id)
      .join(NonSellableInventorySummaries).on(_._1._1._1.nonSellableId === _.id)
  if (summary.skuId === skuId && summary.warehouseId === warehouseId)
  } yield (sellable, preorder, backorder, nonsellable)

  def findSellableBySkuIdInWarehouse(skuId: Int, warehouseId: Int): SellableInventorySummaries.QuerySeq = for {
    sellable ← SellableInventorySummaries 
    invSums ← InventorySummaries.filter(_.sellableId === sellable.id)
                                .filter(_.skuId === skuId)
                                .filter(_.warehouseId === warehouseId)
  } yield sellable
}
