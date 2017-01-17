package models.objects

import java.time.Instant

import shapeless._

import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._
import models.objects.ObjectHeadLinks._

case class ProductVariantLink(id: Int = 0,
                              leftId: Int,
                              rightId: Int,
                              createdAt: Instant = Instant.now,
                              updatedAt: Instant = Instant.now)
    extends FoxModel[ProductVariantLink]
    with ObjectHeadLink[ProductVariantLink]

class ProductVariantLinks(tag: Tag)
    extends ObjectHeadLinks[ProductVariantLink](tag, "product_variant_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductVariantLink.apply _).tupled, ProductVariantLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Variants.tableName, rightId, Variants)(_.id)
}

object ProductVariantLinks
    extends ObjectHeadLinkQueries[ProductVariantLink, ProductVariantLinks, Product, Variant](
      new ProductVariantLinks(_),
      Products,
      Variants)
    with ReturningId[ProductVariantLink, ProductVariantLinks] {

  val returningLens: Lens[ProductVariantLink, Int] = lens[ProductVariantLink].id

  def build(left: Product, right: Variant) =
    ProductVariantLink(leftId = left.id, rightId = right.id)
}
