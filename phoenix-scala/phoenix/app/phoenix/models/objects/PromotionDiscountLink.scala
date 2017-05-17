package phoenix.models.objects

import java.time.Instant

import models.objects.ObjectHeadLinks._
import phoenix.models.discount.{Discount, Discounts}
import phoenix.models.promotion._
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.db._

case class PromotionDiscountLink(id: Int = 0,
                                 leftId: Int,
                                 rightId: Int,
                                 createdAt: Instant = Instant.now,
                                 updatedAt: Instant = Instant.now)
    extends FoxModel[PromotionDiscountLink]
    with ObjectHeadLink[PromotionDiscountLink]

class PromotionDiscountLinks(tag: Tag)
    extends ObjectHeadLinks[PromotionDiscountLink](tag, "promotion_discount_links") {

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((PromotionDiscountLink.apply _).tupled, PromotionDiscountLink.unapply)

  def left  = foreignKey(Promotions.tableName, leftId, Promotions)(_.id)
  def right = foreignKey(Discounts.tableName, rightId, Discounts)(_.id)
}

object PromotionDiscountLinks
    extends ObjectHeadLinkQueries[PromotionDiscountLink,
                                  PromotionDiscountLinks,
                                  Promotion,
                                  Discount](new PromotionDiscountLinks(_), Promotions, Discounts)
    with ReturningId[PromotionDiscountLink, PromotionDiscountLinks] {

  val returningLens: Lens[PromotionDiscountLink, Int] = lens[PromotionDiscountLink].id

  def build(left: Promotion, right: Discount) =
    PromotionDiscountLink(leftId = left.id, rightId = right.id)
}
