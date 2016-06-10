package models.objects

import java.time.Instant

import shapeless._

import models.inventory._
import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class ProductSkuLink(id: Int = 0,
                          leftId: Int,
                          rightId: Int,
                          createdAt: Instant = Instant.now,
                          updatedAt: Instant = Instant.now)
    extends FoxModel[ProductSkuLink]

class ProductSkuLinks(tag: Tag) extends FoxTable[ProductSkuLink](tag, "product_sku_links") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId    = column[Int]("left_id")
  def rightId   = column[Int]("right_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductSkuLink.apply _).tupled, ProductSkuLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Skus.tableName, rightId, Skus)(_.id)
}

object ProductSkuLinks
    extends FoxTableQuery[ProductSkuLink, ProductSkuLinks](new ProductSkuLinks(_))
    with ReturningId[ProductSkuLink, ProductSkuLinks] {

  val returningLens: Lens[ProductSkuLink, Int] = lens[ProductSkuLink].id
}
