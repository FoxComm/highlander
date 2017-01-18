package models.objects

import java.time.Instant

import models.image.Album
import models.objects.ObjectHeadLinks._
import shapeless._

import models.inventory._
import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class ProductSkuLink(id: Int = 0,
                          leftId: Int,
                          rightId: Int,
                          createdAt: Instant = Instant.now,
                          updatedAt: Instant = Instant.now,
                          archivedAt: Option[Instant] = None)
    extends FoxModel[ProductSkuLink]
    with ObjectHeadLink[ProductSkuLink]

class ProductSkuLinks(tag: Tag) extends ObjectHeadLinks[ProductSkuLink](tag, "product_sku_links") {

  def archivedAt: Rep[Option[Instant]] = column[Option[Instant]]("archived_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt, archivedAt) <> ((ProductSkuLink.apply _).tupled, ProductSkuLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Skus.tableName, rightId, Skus)(_.id)
}

object ProductSkuLinks
    extends ObjectHeadLinkQueries[ProductSkuLink, ProductSkuLinks, Product, Sku](
        new ProductSkuLinks(_),
        Products,
        Skus)
    with ReturningId[ProductSkuLink, ProductSkuLinks] {

  val returningLens: Lens[ProductSkuLink, Int] = lens[ProductSkuLink].id

  override def filterLeftId(leftId: Int): QuerySeq =
    super.filterLeftId(leftId).filter(_.archivedAt.isEmpty)
  override def filterRightId(rightId: Int): QuerySeq =
    super.filterRightId(rightId).filter(_.archivedAt.isEmpty)

  def build(left: Product, right: Sku) = ProductSkuLink(leftId = left.id, rightId = right.id)
}
