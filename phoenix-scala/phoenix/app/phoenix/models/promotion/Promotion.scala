package phoenix.models.promotion

import java.time.Instant

import com.github.tminglei.slickpg._
import com.pellucid.sealerate
import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Validation
import objectframework.models._
import phoenix.utils.ADT
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

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
                     scope: LTree,
                     contextId: Int,
                     shadowId: Int,
                     formId: Int,
                     commitId: Int,
                     applyType: Promotion.ApplyType = Promotion.Auto,
                     updatedAt: Instant = Instant.now,
                     createdAt: Instant = Instant.now,
                     archivedAt: Option[Instant] = None)
    extends FoxModel[Promotion]
    with Validation[Promotion]
    with ObjectHead[Promotion] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Promotion =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class Promotions(tag: Tag) extends ObjectHeads[Promotion](tag, "promotions") {

  def applyType = column[Promotion.ApplyType]("apply_type")

  def * =
    (id, scope, contextId, shadowId, formId, commitId, applyType, updatedAt, createdAt, archivedAt) <> ((Promotion.apply _).tupled, Promotion.unapply)
}

object Promotions
    extends ObjectHeadsQueries[Promotion, Promotions](new Promotions(_))
    with ReturningId[Promotion, Promotions] {

  val returningLens: Lens[Promotion, Int] = lens[Promotion].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.formId === formId)

  def filterByContextAndShadowId(contextId: Int, shadowId: Int): QuerySeq =
    filter(_.contextId === contextId).filter(_.shadowId === shadowId)

  object scope {
    implicit class PromotionQuerySeqConversions(q: QuerySeq) {
      def autoApplied: QuerySeq =
        q.filter(_.applyType === (Promotion.Auto: Promotion.ApplyType))

      def couponOnly: QuerySeq =
        q.filter(_.applyType === (Promotion.Coupon: Promotion.ApplyType))
    }
  }
}
