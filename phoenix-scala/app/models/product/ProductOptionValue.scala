package models.product

import java.time.Instant

import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}

import com.github.tminglei.slickpg._

object ProductOptionValue {
  val kind = "product-option-value"
}

case class ProductOptionValue(id: Int = 0,
                              scope: LTree,
                              contextId: Int,
                              shadowId: Int,
                              formId: Int,
                              commitId: Int,
                              updatedAt: Instant = Instant.now,
                              createdAt: Instant = Instant.now,
                              archivedAt: Option[Instant] = None)
    extends FoxModel[ProductOptionValue]
    with Validation[ProductOptionValue]
    with ObjectHead[ProductOptionValue] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): ProductOptionValue =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class ProductOptionValues(tag: Tag)
    extends ObjectHeads[ProductOptionValue](tag, "product_option_values") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((ProductOptionValue.apply _).tupled,
        ProductOptionValue.unapply)
}

object ProductOptionValues
    extends FoxTableQuery[ProductOptionValue, ProductOptionValues](new ProductOptionValues(_))
    with ReturningId[ProductOptionValue, ProductOptionValues] {

  val returningLens: Lens[ProductOptionValue, Int] = lens[ProductOptionValue].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filterByContext(contextId).filter(_.formId === formId)

  def filterByContextAndFormIds(contextId: Int, formIds: Seq[Int]): QuerySeq =
    filterByContext(contextId).filter(_.id.inSet(formIds))
}
