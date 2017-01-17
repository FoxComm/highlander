package models.cord

import java.time.Instant

import cats.implicits._
import models.coupon.CouponCode
import models.objects._
import models.promotion.Promotion
import models.promotion.Promotion._
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

final case class OrderPromotion(id: Int = 0,
                                cordRef: String,
                                promotionShadowId: Int,
                                applyType: Promotion.ApplyType,
                                couponCodeId: Option[Int] = None,
                                createdAt: Instant = Instant.now)
    extends FoxModel[OrderPromotion]

object OrderPromotion {

  def buildAuto(cart: Cart, promo: Promotion): OrderPromotion =
    OrderPromotion(cordRef = cart.refNum,
                   promotionShadowId = promo.shadowId,
                   applyType = Promotion.Auto)

  def buildCoupon(cart: Cart, promo: Promotion, code: CouponCode): OrderPromotion =
    OrderPromotion(cordRef = cart.refNum,
                   promotionShadowId = promo.shadowId,
                   applyType = Promotion.Coupon,
                   couponCodeId = code.id.some)
}

class OrderPromotions(tag: Tag) extends FoxTable[OrderPromotion](tag, "order_promotions") {
  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef           = column[String]("cord_ref")
  def promotionShadowId = column[Int]("promotion_shadow_id")
  def applyType         = column[Promotion.ApplyType]("apply_type")
  def couponCodeId      = column[Option[Int]]("coupon_code_id")
  def createdAt         = column[Instant]("created_at")

  def * =
    (id, cordRef, promotionShadowId, applyType, couponCodeId, createdAt) <> ((OrderPromotion.apply _).tupled,
    OrderPromotion.unapply)

  def order           = foreignKey(Carts.tableName, cordRef, Carts)(_.referenceNumber)
  def promotionShadow = foreignKey(ObjectShadows.tableName, promotionShadowId, ObjectShadows)(_.id)
}

object OrderPromotions
    extends FoxTableQuery[OrderPromotion, OrderPromotions](new OrderPromotions(_))
    with ReturningId[OrderPromotion, OrderPromotions] {

  val returningLens: Lens[OrderPromotion, Int] = lens[OrderPromotion].id

  def filterByCordRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

  def filterByOrderRefAndShadows(cordRef: String, shadows: Seq[Int]): QuerySeq =
    filter(_.cordRef === cordRef).filter(_.promotionShadowId.inSet(shadows))

  object scope {
    implicit class OrderPromotionQuerySeqConversions(q: QuerySeq) {
      def autoApplied: QuerySeq =
        q.filter(_.applyType === (Promotion.Auto: Promotion.ApplyType))

      def requiresCoupon: QuerySeq =
        q.filter(_.applyType === (Promotion.Coupon: Promotion.ApplyType))
    }
  }
}
