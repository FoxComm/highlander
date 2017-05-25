package phoenix.models.coupon

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import shapeless._

/**
  * This keeps track of how many times a particular coupon was used.
  */
final case class CouponUsage(id: Int = 0,
                             couponFormId: Int,
                             count: Int = 0,
                             updatedAt: Instant = Instant.now,
                             createdAt: Instant = Instant.now)
    extends FoxModel[CouponUsage]

class CouponUsages(tag: Tag) extends FoxTable[CouponUsage](tag, "coupon_usages") {

  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def couponFormId = column[Int]("coupon_form_id")
  def count        = column[Int]("count")
  def updatedAt    = column[Instant]("updated_at")
  def createdAt    = column[Instant]("created_at")

  def * =
    (id, couponFormId, count, updatedAt, createdAt) <> ((CouponUsage.apply _).tupled, CouponUsage.unapply)
}

object CouponUsages
    extends FoxTableQuery[CouponUsage, CouponUsages](new CouponUsages(_))
    with ReturningId[CouponUsage, CouponUsages] {

  val returningLens: Lens[CouponUsage, Int] = lens[CouponUsage].id

  def filterByCoupon(couponFormId: Int): QuerySeq =
    filter(_.couponFormId === couponFormId)
}
