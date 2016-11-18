package models.objects

import java.time.Instant

import models.image.Album
import models.objects.ObjectHeadLinks._
import shapeless._
import models.inventory._
import models.product._
import models.taxonomy.{Taxon, Taxons}
import utils.db._
import utils.db.ExPostgresDriver.api._

case class SkuTaxonLink(id: Int = 0,
                        leftId: Int,
                        rightId: Int,
                        createdAt: Instant = Instant.now,
                        updatedAt: Instant = Instant.now)
    extends FoxModel[SkuTaxonLink]
    with ObjectHeadLink[SkuTaxonLink]

class SkuTaxonLinks(tag: Tag) extends ObjectHeadLinks[SkuTaxonLink](tag, "sku_taxon_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((SkuTaxonLink.apply _).tupled, SkuTaxonLink.unapply)
}

object SkuTaxonLinks
    extends ObjectHeadLinkQueries[SkuTaxonLink, SkuTaxonLinks, Sku, Taxon](new SkuTaxonLinks(_),
                                                                           Skus,
                                                                           Taxons)
    with ReturningId[SkuTaxonLink, SkuTaxonLinks] {

  val returningLens: Lens[SkuTaxonLink, Int] = lens[SkuTaxonLink].id

  def build(left: Sku, right: Taxon) = SkuTaxonLink(leftId = left.id, rightId = right.id)
}
