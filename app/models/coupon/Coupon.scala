package models.coupon

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant

object Coupon {
  val kind = "coupon"
}

/**
 * A Coupon is a way to share a Promotion that isn't publicicly available.
 * Which Promotion the coupon points is determined by an ObjectLink
 */
final case class Coupon(id: Int = 0, code: String, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Coupon]
  with Validation[Coupon]

class Coupons(tag: Tag) extends ObjectHeads[Coupon](tag, "coupons") {

  def code = column[String]("code")

  def * = (id, code, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Coupon.apply _).tupled, Coupon.unapply)

}

object Coupons extends TableQueryWithId[Coupon, Coupons](
  idLens = GenLens[Coupon](_.id))(new Coupons(_)) {

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)
}
