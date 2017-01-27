package models.product

import java.time.Instant
import models.inventory.{ProductVariant, ProductVariants}
import models.objects.ObjectHeadLinks.{ObjectHeadLink, ObjectHeadLinkQueries, ObjectHeadLinks}
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ProductValueVariantLink(id: Int = 0,
                                   leftId: Int,
                                   rightId: Int,
                                   createdAt: Instant = Instant.now,
                                   updatedAt: Instant = Instant.now)
    extends FoxModel[ProductValueVariantLink]
    with ObjectHeadLink[ProductValueVariantLink]

class ProductValueVariantLinks(tag: Tag)
    extends ObjectHeadLinks[ProductValueVariantLink](tag, "product_value_to_variant_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductValueVariantLink.apply _).tupled, ProductValueVariantLink.unapply)

  def left  = foreignKey(ProductOptionValues.tableName, leftId, ProductOptionValues)(_.id)
  def right = foreignKey(ProductVariants.tableName, rightId, ProductVariants)(_.id)
}

object ProductValueVariantLinks
    extends ObjectHeadLinkQueries[ProductValueVariantLink,
                                  ProductValueVariantLinks,
                                  ProductOptionValue,
                                  ProductVariant](new ProductValueVariantLinks(_),
                                                  ProductOptionValues,
                                                  ProductVariants)
    with ReturningId[ProductValueVariantLink, ProductValueVariantLinks] {

  val returningLens: Lens[ProductValueVariantLink, Int] = lens[ProductValueVariantLink].id

  def findProductVariantsForProductValues(
      productValueHeadIds: Seq[Int]): Query[(Rep[Int], Rep[String]), (Int, String), Seq] = {
    for {
      link    ← filter(_.leftId inSet productValueHeadIds)
      variant ← ProductVariants if link.rightId === variant.id
    } yield (link.leftId, variant.code)
  }

  def build(left: ProductOptionValue, right: ProductVariant): ProductValueVariantLink =
    ProductValueVariantLink(leftId = left.id, rightId = right.id)
}
