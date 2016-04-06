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

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)

  def filterByContextAndCode(contextId: Int, code: String): QuerySeq = 
    filter(_.contextId === contextId).filter(_.code.toLowerCase === code.toLowerCase)
}
