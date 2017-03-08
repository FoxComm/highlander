package models.objects

import java.time.Instant

import models.image.Album
import models.objects.ObjectHeadLinks._
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
    with ObjectHeadLink[ProductVariantLink]

class ProductVariantLinks(tag: Tag)
    extends ObjectHeadLinks[ProductVariantLink](tag, "product_to_variant_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductVariantLink.apply _).tupled, ProductVariantLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(ProductVariants.tableName, rightId, ProductVariants)(_.id)
}

object ProductVariantLinks
    extends ObjectHeadLinkQueries[ProductVariantLink,
                                  ProductVariantLinks,
                                  Product,
                                  ProductVariant](new ProductVariantLinks(_),
                                                  Products,
                                                  ProductVariants)
    with ReturningId[ProductVariantLink, ProductVariantLinks] {

  val returningLens: Lens[ProductVariantLink, Int] = lens[ProductVariantLink].id

  def build(left: Product, right: ProductVariant) =
    ProductVariantLink(leftId = left.id, rightId = right.id)
}
