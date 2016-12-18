package models.objects

import java.time.Instant

import shapeless._

import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._
import models.objects.ObjectHeadLinks._

case class ProductOptionLink(id: Int = 0,
                             leftId: Int,
                             rightId: Int,
                             createdAt: Instant = Instant.now,
                             updatedAt: Instant = Instant.now)
    extends FoxModel[ProductOptionLink]
    with ObjectHeadLink[ProductOptionLink]

class ProductOptionLinks(tag: Tag)
    extends ObjectHeadLinks[ProductOptionLink](tag, "product__option_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductOptionLink.apply _).tupled, ProductOptionLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(ProductOptions.tableName, rightId, ProductOptions)(_.id)
}

object ProductOptionLinks
    extends ObjectHeadLinkQueries[ProductOptionLink, ProductOptionLinks, Product, ProductOption](
        new ProductOptionLinks(_),
        Products,
        ProductOptions)
    with ReturningId[ProductOptionLink, ProductOptionLinks] {

  val returningLens: Lens[ProductOptionLink, Int] = lens[ProductOptionLink].id

  def build(left: Product, right: ProductOption) =
    ProductOptionLink(leftId = left.id, rightId = right.id)
}
