package phoenix.models.coupon

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import shapeless._

/**
  * This keeps track of how many times a particular coupon was used by a account.
  */
final case class CouponCustomerUsage(id: Int = 0,
                                     couponFormId: Int,
                                     accountId: Int,
                                     count: Int = 0,
                                     updatedAt: Instant = Instant.now,
                                     createdAt: Instant = Instant.now)
    extends FoxModel[CouponCustomerUsage]

class CouponCustomerUsages(tag: Tag) extends FoxTable[CouponCustomerUsage](tag, "coupon_customer_usages") {

  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def couponFormId = column[Int]("coupon_form_id")
  def accountId    = column[Int]("account_id")
  def count        = column[Int]("count")
  def updatedAt    = column[Instant]("updated_at")
  def createdAt    = column[Instant]("created_at")

  def * =
    (id, couponFormId, accountId, count, updatedAt, createdAt) <> ((CouponCustomerUsage.apply _).tupled, CouponCustomerUsage.unapply)
}

object CouponCustomerUsages
    extends FoxTableQuery[CouponCustomerUsage, CouponCustomerUsages](new CouponCustomerUsages(_))
    with ReturningId[CouponCustomerUsage, CouponCustomerUsages] {

  val returningLens: Lens[CouponCustomerUsage, Int] = lens[CouponCustomerUsage].id

  def filterByCoupon(couponFormId: Int): QuerySeq =
    filter(_.couponFormId === couponFormId)

  def filterByCouponAndAccount(couponFormId: Int, accountId: Int): QuerySeq =
    filterByCoupon(couponFormId).filter(_.accountId === accountId)
}
