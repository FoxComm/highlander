package models.objects

import java.time.Instant

import shapeless._

import models.inventory._
import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class ProductVariantLink(id: Int = 0,
                              leftId: Int,
                              rightId: Int,
                              createdAt: Instant = Instant.now,
                              updatedAt: Instant = Instant.now)
    extends FoxModel[ProductVariantLink]

class ProductVariantLinks(tag: Tag)
    extends FoxTable[ProductVariantLink](tag, "product_variant_links") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId    = column[Int]("left_id")
  def rightId   = column[Int]("right_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductVariantLink.apply _).tupled, ProductVariantLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Variants.tableName, rightId, Variants)(_.id)
}

object ProductVariantLinks
    extends FoxTableQuery[ProductVariantLink, ProductVariantLinks](new ProductVariantLinks(_))
    with ReturningId[ProductVariantLink, ProductVariantLinks] {

  val returningLens: Lens[ProductVariantLink, Int] = lens[ProductVariantLink].id
}
