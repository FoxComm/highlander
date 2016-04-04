package models.promotion

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import java.time.Instant

object Promotion {
  val kind = "promotion"
}

/**
 * A Promotion is a way to bundle several discounts into a presentable form.
 * ObjectLinks are used to connect a promotion to several discounts.
 */
final case class Promotion(id: Int = 0, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, requireCoupon: Boolean = false, updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Promotion]
  with Validation[Promotion]

class Promotions(tag: Tag) extends ObjectHeads[Promotion](tag, "promotions") {


  def requireCoupon = column[Boolean]("require_coupon")

  def * = (id, contextId, shadowId, formId, commitId, requireCoupon, updatedAt, createdAt) <> ((Promotion.apply _).tupled, Promotion.unapply)

}

object Promotions extends TableQueryWithId[Promotion, Promotions](
  idLens = GenLens[Promotion](_.id))(new Promotions(_)) {

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)
}
