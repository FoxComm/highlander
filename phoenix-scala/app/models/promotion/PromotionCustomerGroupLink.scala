package models.promotion

import java.time.Instant

import models.customer.{CustomerGroup, CustomerGroups}
import models.objects.ObjectHeadLinks._
import shapeless._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class PromotionCustomerGroupLink(id: Int = 0,
                                      leftId: Int,
                                      rightId: Int,
                                      createdAt: Instant = Instant.now,
                                      updatedAt: Instant = Instant.now)
    extends FoxModel[PromotionCustomerGroupLink]

class PromotionCustomerGroupLinks(tag: Tag)
    extends FoxTable[PromotionCustomerGroupLink](tag, "promotion_customer_group_links") {
  // FIXME: what an awful lot of duplication @michalrus
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def leftId    = column[Int]("left_id")
  def rightId   = column[Int]("right_id")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * =
    (id, leftId, rightId, createdAt, updatedAt) <> ((PromotionCustomerGroupLink.apply _).tupled, PromotionCustomerGroupLink.unapply)

  def left  = foreignKey(Promotions.tableName, leftId, Promotions)(_.id)
  def right = foreignKey(CustomerGroups.tableName, rightId, CustomerGroups)(_.id)
}

object PromotionCustomerGroupLinks
    extends FoxTableQuery[PromotionCustomerGroupLink, PromotionCustomerGroupLinks](
        new PromotionCustomerGroupLinks(_))
    with ReturningId[PromotionCustomerGroupLink, PromotionCustomerGroupLinks] {

  val returningLens: Lens[PromotionCustomerGroupLink, Int] = lens[PromotionCustomerGroupLink].id

  def build(left: Promotion, right: CustomerGroup) =
    PromotionCustomerGroupLink(leftId = left.id, rightId = right.id)
}
