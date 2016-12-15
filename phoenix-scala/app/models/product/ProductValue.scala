package models.product

import java.time.Instant

import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}

import com.github.tminglei.slickpg._

object ProductValue {
  val kind = "product-value"
}

case class ProductValue(id: Int = 0,
                        scope: LTree,
                        contextId: Int,
                        shadowId: Int,
                        formId: Int,
                        commitId: Int,
                        updatedAt: Instant = Instant.now,
                        createdAt: Instant = Instant.now,
                        archivedAt: Option[Instant] = None)
    extends FoxModel[ProductValue]
    with Validation[ProductValue]
    with ObjectHead[ProductValue] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): ProductValue =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class ProductValues(tag: Tag) extends ObjectHeads[ProductValue](tag, "product_values") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((ProductValue.apply _).tupled,
        ProductValue.unapply)
}

object ProductValues
    extends FoxTableQuery[ProductValue, ProductValues](new ProductValues(_))
    with ReturningId[ProductValue, ProductValues] {

  val returningLens: Lens[ProductValue, Int] = lens[ProductValue].id

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
