package phoenix.models.coupon

import java.time.Instant

import shapeless._
import core.db.ExPostgresDriver.api._
import core.db._

/**
  * This keeps track of how many times a particular coupon was used by a customer.
  */
final case class CouponCodeUsage(id: Int = 0,
                                 couponFormId: Int,
                                 couponCodeId: Int,
                                 count: Int = 0,
                                 updatedAt: Instant = Instant.now,
                                 createdAt: Instant = Instant.now)
    extends FoxModel[CouponCodeUsage]

class CouponCodeUsages(tag: Tag) extends FoxTable[CouponCodeUsage](tag, "coupon_code_usages") {

  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def couponFormId = column[Int]("coupon_form_id")
  def couponCodeId = column[Int]("coupon_code_id")
  def count        = column[Int]("count")
  def updatedAt    = column[Instant]("updated_at")
  def createdAt    = column[Instant]("created_at")

  def * =
    (id, couponFormId, couponCodeId, count, updatedAt, createdAt) <> ((CouponCodeUsage.apply _).tupled, CouponCodeUsage.unapply)
}

object CouponCodeUsages
    extends FoxTableQuery[CouponCodeUsage, CouponCodeUsages](new CouponCodeUsages(_))
    with ReturningId[CouponCodeUsage, CouponCodeUsages] {

  val returningLens: Lens[CouponCodeUsage, Int] = lens[CouponCodeUsage].id

  def filterByCoupon(couponFormId: Int): QuerySeq =
    filter(_.couponFormId === couponFormId)

  def filterByCouponAndCode(couponFormId: Int, couponCodeId: Int): QuerySeq =
    filterByCoupon(couponFormId).filter(_.couponCodeId === couponCodeId)
}
