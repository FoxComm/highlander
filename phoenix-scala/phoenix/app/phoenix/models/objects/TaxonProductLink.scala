package phoenix.models.objects

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import objectframework.models.ObjectHeadLinks._
import phoenix.models.product._
import phoenix.models.taxonomy.{Taxon, Taxons}
import shapeless._

case class ProductTaxonLink(id: Int = 0,
                            leftId: Int,
                            rightId: Int,
                            createdAt: Instant = Instant.now,
                            updatedAt: Instant = Instant.now,
                            archivedAt: Option[Instant] = None)
    extends FoxModel[ProductTaxonLink]
    with ObjectHeadLink[ProductTaxonLink]

class ProductTaxonLinks(tag: Tag) extends ObjectHeadLinks[ProductTaxonLink](tag, "product_taxon_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt, archivedAt) <> ((ProductTaxonLink.apply _).tupled, ProductTaxonLink.unapply)

  def left  = foreignKey(Products.tableName, leftId, Products)(_.id)
  def right = foreignKey(Taxons.tableName, rightId, Taxons)(_.id)
}

object ProductTaxonLinks
    extends ObjectHeadLinkQueries[ProductTaxonLink, ProductTaxonLinks, Product, Taxon](
      new ProductTaxonLinks(_),
      Products,
      Taxons)
    with ReturningId[ProductTaxonLink, ProductTaxonLinks] {

  val returningLens: Lens[ProductTaxonLink, Int] = lens[ProductTaxonLink].id

  def build(left: Product, right: Taxon) = ProductTaxonLink(leftId = left.id, rightId = right.id)
}
