package models.objects

import java.time.Instant

import shapeless._

import models.product._
import utils.db._
import utils.db.ExPostgresDriver.api._
import models.objects.ObjectHeadLinks._

case class ProductOptionValueLink(id: Int = 0,
                                  leftId: Int,
                                  rightId: Int,
                                  createdAt: Instant = Instant.now,
                                  updatedAt: Instant = Instant.now)
    extends FoxModel[ProductOptionValueLink]
    with ObjectHeadLink[ProductOptionValueLink]

class ProductOptionValueLinks(tag: Tag)
    extends ObjectHeadLinks[ProductOptionValueLink](tag, "product_option_to_value_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((ProductOptionValueLink.apply _).tupled, ProductOptionValueLink.unapply)

  def left  = foreignKey(ProductOptions.tableName, leftId, ProductOptions)(_.id)
  def right = foreignKey(ProductOptionValues.tableName, rightId, ProductOptionValues)(_.id)
}

object ProductOptionValueLinks
    extends ObjectHeadLinkQueries[ProductOptionValueLink,
                                  ProductOptionValueLinks,
                                  ProductOption,
                                  ProductOptionValue](new ProductOptionValueLinks(_),
                                                      ProductOptions,
                                                      ProductOptionValues)
    with ReturningId[ProductOptionValueLink, ProductOptionValueLinks] {

  val returningLens: Lens[ProductOptionValueLink, Int] = lens[ProductOptionValueLink].id

  def build(left: ProductOption, right: ProductOptionValue): ProductOptionValueLink =
    ProductOptionValueLink(leftId = left.id, rightId = right.id)
}
