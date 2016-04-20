package models.coupon

import java.time.Instant

import models.objects._
import monocle.macros.GenLens
import slick.lifted.Tag
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

object Coupon {
  val kind = "coupon"
}

/**
 * A Coupon is a way to share a Promotion that isn't publicicly available.
 */
case class Coupon(id: Int = 0, promotionId: Int, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends FoxModel[Coupon]
  with Validation[Coupon]

class Coupons(tag: Tag) extends ObjectHeads[Coupon](tag, "coupons") {

  def promotionId = column[Int]("promotion_id")

  def * = (id, promotionId, contextId, shadowId, formId, commitId, updatedAt, createdAt) <> ((Coupon.apply _).tupled, Coupon.unapply)

}

object Coupons extends FoxTableQuery[Coupon, Coupons](
  idLens = GenLens[Coupon](_.id))(new Coupons(_)) {

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq = 
    filter(_.contextId === contextId).filter(_.formId === formId)
}
