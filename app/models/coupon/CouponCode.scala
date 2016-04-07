package models.coupon

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant

/**
 * A coupon code is a way to reference a coupon from the outside world. 
 * Multiple codes may point to the same coupon.
 */
final case class CouponCode(id: Int = 0, code: String, couponFormId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[CouponCode]
  with Validation[CouponCode]

class CouponCodes(tag: Tag) extends ObjectHeads[CouponCode](tag, "coupon_codes") {

  def code = column[String]("code")
  def couponFormId = column[Int]("coupon_form_id")

  def * = (id, code, couponFormId, createdAt) <> ((CouponCode.apply _).tupled, CouponCode.unapply)

}

object CouponCodes extends TableQueryWithId[CouponCode, CouponCodes](
  idLens = GenLens[CouponCode](_.id))(new CouponCodes(_)) {

  def generateCode(couponFormId: Int, prefix: String, number: Int, leadingZeros: Int) : CouponCode = {
    //TODO, We should add some salt here so that people can't guess the coupon code.
    //and create a hash, turn it into a num and put it after the prefix.
    val num = s"%0${leadingZeros}d".format(number)
    val code = s"${prefix}${couponFormId}${num}"
    CouponCode(code = code, couponFormId = couponFormId)
  }

  def generateCodes(couponFormId: Int, prefix: String, leadingZeros: Int, count: Int) :
    Seq[CouponCode] = (1 to count).map { i ⇒ generateCode(couponFormId, prefix, i, leadingZeros) } 

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)

  def filterByContextAndCode(contextId: Int, code: String): QuerySeq = 
    filter(_.contextId === contextId).filter(_.code.toLowerCase === code.toLowerCase)
}
