package models.objects

import java.time.Instant

import shapeless._

import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._
import models.objects.ObjectHeadLinks._

case class VariantValueLink(id: Int = 0,
                            leftId: Int,
                            rightId: Int,
                            createdAt: Instant = Instant.now,
                            updatedAt: Instant = Instant.now)
    extends FoxModel[VariantValueLink]
    with ObjectHeadLink[VariantValueLink]

class VariantValueLinks(tag: Tag)
    extends ObjectHeadLinks[VariantValueLink](tag, "variant_variant_value_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((VariantValueLink.apply _).tupled, VariantValueLink.unapply)

  def left  = foreignKey(Variants.tableName, leftId, Variants)(_.id)
  def right = foreignKey(VariantValues.tableName, rightId, VariantValues)(_.id)
}

object VariantValueLinks
    extends ObjectHeadLinkQueries[VariantValueLink, VariantValueLinks, Variant, VariantValue](
      new VariantValueLinks(_),
      Variants,
      VariantValues)
    with ReturningId[VariantValueLink, VariantValueLinks] {

  val returningLens: Lens[VariantValueLink, Int] = lens[VariantValueLink].id

  def build(left: Variant, right: VariantValue) =
    VariantValueLink(leftId = left.id, rightId = right.id)
}
