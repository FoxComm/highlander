package phoenix.models.product

import java.time.Instant

import com.github.tminglei.slickpg._
import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Validation
import objectframework.models._
import phoenix.utils.JsonFormatters
import shapeless._
import slick.lifted.Tag

object VariantValue {
  val kind = "variant-value"
}

case class VariantValue(id: Int = 0,
                        scope: LTree,
                        contextId: Int,
                        shadowId: Int,
                        formId: Int,
                        commitId: Int,
                        updatedAt: Instant = Instant.now,
                        createdAt: Instant = Instant.now,
                        archivedAt: Option[Instant] = None)
    extends FoxModel[VariantValue]
    with Validation[VariantValue]
    with ObjectHead[VariantValue] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): VariantValue =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class VariantValues(tag: Tag) extends ObjectHeads[VariantValue](tag, "variant_values") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((VariantValue.apply _).tupled,
    VariantValue.unapply)
}

object VariantValues
    extends FoxTableQuery[VariantValue, VariantValues](new VariantValues(_))
    with ReturningId[VariantValue, VariantValues] {

  val returningLens: Lens[VariantValue, Int] = lens[VariantValue].id

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
