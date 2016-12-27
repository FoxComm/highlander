package models.product

import java.time.Instant

import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}

import com.github.tminglei.slickpg._

object ProductOption {
  val kind = "product-option"
}

/**
  * A ProductOption represents the attributes that are used to collect SKUs into a
  * product. One ProductOption will contain an array of many values, which define the
  * individual attributes that can be used to select a SKU.
  */
case class ProductOption(id: Int = 0,
                         scope: LTree,
                         contextId: Int,
                         shadowId: Int,
                         formId: Int,
                         commitId: Int,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         archivedAt: Option[Instant] = None)
    extends FoxModel[ProductOption]
    with Validation[ProductOption]
    with ObjectHead[ProductOption] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): ProductOption =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class ProductOptions(tag: Tag) extends ObjectHeads[ProductOption](tag, "product_options") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((ProductOption.apply _).tupled, ProductOption.unapply)
}

object ProductOptions
    extends FoxTableQuery[ProductOption, ProductOptions](new ProductOptions(_))
    with ReturningId[ProductOption, ProductOptions] {

  val returningLens: Lens[ProductOption, Int] = lens[ProductOption].id

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByContextAndFormIds(contextId: Int, formIds: Seq[Int]): QuerySeq =
    filterByContext(contextId).filter(_.id.inSet(formIds))

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filterByContext(contextId).filter(_.formId === formId)
}
