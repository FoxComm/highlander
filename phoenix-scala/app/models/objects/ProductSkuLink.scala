package models.objects

import java.time.Instant

import models.objects.ObjectHeadLinks._
import models.product.VariantValueSkuLinks._
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
    with ObjectHeadLink[ProductSkuLink]

class ProductSkuLinks(tag: Tag) extends ObjectHeadLinks[ProductSkuLink](tag, "product_sku_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductSkuLink.apply _).tupled, ProductSkuLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Skus.tableName, rightId, Skus)(_.id)
}

object ProductSkuLinks
    extends ObjectHeadLinkQueries[ProductSkuLink, ProductSkuLinks](new ProductSkuLinks(_))
    with ReturningId[ProductSkuLink, ProductSkuLinks] {

  val returningLens: Lens[ProductSkuLink, Int] = lens[ProductSkuLink].id
}
