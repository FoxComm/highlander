package models.coupon

import shapeless._
import utils.Validation
import utils.db._
import utils.db.ExPostgresDriver.api._

import java.time.Instant

/**
 * This keeps track of how many times a particular coupon was used by a customer.
 */
final case class CouponCustomerUsage(id: Int = 0, couponFormId: Int, customerId: Int, 
  count: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends FoxModel[CouponCustomerUsage]
  with Validation[CouponCustomerUsage]

class CouponCustomerUsages(tag: Tag) extends FoxTable[CouponCustomerUsage](tag, "coupon_customer_usages")  {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def couponFormId = column[Int]("coupon_form_id")
  def customerId = column[Int]("customer_id")
  def count = column[Int]("count")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")

  def * = (id, couponFormId, customerId, count, updatedAt, createdAt) <> ((CouponCustomerUsage.apply _).tupled, CouponCustomerUsage.unapply)

}

object CouponCustomerUsages extends FoxTableQuery[CouponCustomerUsage, CouponCustomerUsages](new CouponCustomerUsages(_))
  with ReturningId[CouponCustomerUsage, CouponCustomerUsages] {

  val returningLens: Lens[CouponCustomerUsage, Int] = lens[CouponCustomerUsage].id

  def filterByCoupon(couponFormId: Int): QuerySeq = 
    filter(_.couponFormId === couponFormId)

}
