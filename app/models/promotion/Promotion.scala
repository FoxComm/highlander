package models.promotion

import models.Aliases.Json
import models.objects._

import monocle.macros.GenLens
import utils.ExPostgresDriver.api._
import utils.JsonFormatters
import utils.time.JavaTimeSlickMapper._
import utils.{ADT, GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

import slick.jdbc.JdbcType
import slick.ast.BaseTypedType
import com.pellucid.sealerate
import java.time.Instant

object Promotion {
  val kind = "promotion"

  sealed trait ApplyType

  case object Auto extends ApplyType
  case object Coupon extends ApplyType

  object ApplyType extends ADT[ApplyType] {
    def types = sealerate.values[ApplyType]
  }

  implicit val stateColumnType: JdbcType[ApplyType] with BaseTypedType[ApplyType] = ApplyType.slickColumn

}

/**
 * A Promotion is a way to bundle several discounts into a presentable form.
 * ObjectLinks are used to connect a promotion to several discounts.
 */
case class Promotion(id: Int = 0, contextId: Int, shadowId: Int, formId: Int, 
  commitId: Int, applyType: Promotion.ApplyType = Promotion.Auto, 
  updatedAt: Instant = Instant.now, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Promotion]
  with Validation[Promotion]

class Promotions(tag: Tag) extends ObjectHeads[Promotion](tag, "promotions") {


  def requireCoupon = column[Promotion.ApplyType]("apply_type")

  def * = (id, contextId, shadowId, formId, commitId, requireCoupon, updatedAt, createdAt) <> ((Promotion.apply _).tupled, Promotion.unapply)

}

object Promotions extends TableQueryWithId[Promotion, Promotions](
  idLens = GenLens[Promotion](_.id))(new Promotions(_)) {

  def filterByContext(contextId: Int): QuerySeq = 
    filter(_.contextId === contextId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq = 
    filter(_.contextId === contextId).filter(_.formId === formId)
}
