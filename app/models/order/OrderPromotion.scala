package models.order

import models.objects._
import models.promotion.Promotion
import models.promotion.Promotion._
import java.time.Instant
import shapeless._
import utils.db._

import slick.driver.PostgresDriver.api._

final case class OrderPromotion(id: Int = 0, orderId: Int = 0, promotionShadowId: Int,
  applyType: Promotion.ApplyType, createdAt: Instant = Instant.now) extends FoxModel[OrderPromotion]

object OrderPromotion {

  def buildAuto(order: Order, promo: Promotion): OrderPromotion = OrderPromotion(orderId = order.id,
    promotionShadowId = promo.shadowId, applyType = Promotion.Auto)

  def buildCoupon(order: Order, promo: Promotion): OrderPromotion = OrderPromotion(orderId = order.id,
    promotionShadowId = promo.shadowId, applyType = Promotion.Coupon)
}

class OrderPromotions(tag: Tag) extends FoxTable[OrderPromotion](tag, "order_promotions") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def promotionShadowId = column[Int]("promotion_shadow_id")
  def applyType = column[Promotion.ApplyType]("apply_type")
  def createdAt = column[Instant]("created_at")

  def * = (id, orderId, promotionShadowId,
    applyType, createdAt) <> ((OrderPromotion.apply _).tupled, OrderPromotion.unapply)

  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def promotionShadow = foreignKey(ObjectShadows.tableName, promotionShadowId, ObjectShadows)(_.id)
}

object OrderPromotions extends FoxTableQuery[OrderPromotion, OrderPromotions](new OrderPromotions(_))
  with ReturningId[OrderPromotion, OrderPromotions] {

  val returningLens: Lens[OrderPromotion, Int] = lens[OrderPromotion].id

  def filterByOrderId(orderId: Int): QuerySeq =
    filter(_.orderId === orderId)

  def filterByOrderIdAndShadows(orderId: Int, shadows: Seq[Int]): QuerySeq =
    filter(_.orderId === orderId).filter(_.promotionShadowId.inSet(shadows))

  object scope {
    implicit class OrderPromotionQuerySeqConversions(q: QuerySeq) {
      def autoApplied: QuerySeq =
        q.filter(_.applyType === (Promotion.Auto: Promotion.ApplyType))

      def requiresCoupon: QuerySeq =
        q.filter(_.applyType === (Promotion.Coupon: Promotion.ApplyType))
    }
  }
}
