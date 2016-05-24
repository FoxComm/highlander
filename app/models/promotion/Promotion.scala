package models.promotion

import java.time.Instant

import com.pellucid.sealerate
import models.objects._
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.db.ExPostgresDriver.api._
import utils.db._
import utils.{ADT, Validation}

object Promotion {
  val kind = "promotion"

  sealed trait ApplyType

  case object Auto   extends ApplyType
  case object Coupon extends ApplyType

  object ApplyType extends ADT[ApplyType] {
    def types = sealerate.values[ApplyType]
  }

  implicit val stateColumnType: JdbcType[ApplyType] with BaseTypedType[ApplyType] =
    ApplyType.slickColumn
}

/**
  * A Promotion is a way to bundle several discounts into a presentable form.
  * ObjectLinks are used to connect a promotion to several discounts.
  */
case class Promotion(id: Int = 0,
                     contextId: Int,
                     shadowId: Int,
                     formId: Int,
                     commitId: Int,
                     applyType: Promotion.ApplyType = Promotion.Auto,
                     updatedAt: Instant = Instant.now,
                     createdAt: Instant = Instant.now)
    extends FoxModel[Promotion]
    with Validation[Promotion]

class Promotions(tag: Tag) extends ObjectHeads[Promotion](tag, "promotions") {

  def requireCoupon = column[Promotion.ApplyType]("apply_type")

  def * =
    (id, contextId, shadowId, formId, commitId, requireCoupon, updatedAt, createdAt) <> ((Promotion.apply _).tupled, Promotion.unapply)
}

object Promotions
    extends FoxTableQuery[Promotion, Promotions](new Promotions(_))
    with ReturningId[Promotion, Promotions] {

  val returningLens: Lens[Promotion, Int] = lens[Promotion].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === formId)

  object scope {
    implicit class PromotionQuerySeqConversions(q: QuerySeq) {
      def autoApplied: QuerySeq =
        q.filter(_.requireCoupon === (Promotion.Auto: Promotion.ApplyType))

      def requiresCoupon: QuerySeq =
        q.filter(_.requireCoupon === (Promotion.Coupon: Promotion.ApplyType))
    }
  }
}
