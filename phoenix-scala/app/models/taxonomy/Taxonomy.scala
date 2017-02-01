package models.taxonomy
import java.time.Instant

import com.github.tminglei.slickpg.LTree
import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.Validation
import utils.db._

object Taxonomy {
  val kind = "taxonomy"
}

case class Taxonomy(id: Int = 0,
                    scope: LTree,
                    hierarchical: Boolean,
                    contextId: Int,
                    formId: Int,
                    shadowId: Int,
                    commitId: Int,
                    updatedAt: Instant = Instant.now,
                    createdAt: Instant = Instant.now,
                    archivedAt: Option[Instant] = None)
    extends FoxModel[Taxonomy]
    with Validation[Taxonomy]
    with ObjectHead[Taxonomy] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Taxonomy =
    this.copy(shadowId = shadowId, commitId = commitId)

  def copyForCreate(contextId: Int, formId: Int, shadowId: Int, commitId: Int): Taxonomy =
    this.copy(contextId = contextId, formId = formId, shadowId = shadowId, commitId = commitId)
}

class Taxonomies(tag: Tag) extends ObjectHeads[Taxonomy](tag, "taxonomies") {

  def hierarchical = column[Boolean]("hierarchical")

  def * =
    (id,
     scope,
     hierarchical,
     contextId,
     formId,
     shadowId,
     commitId,
     updatedAt,
     createdAt,
     archivedAt) <> ((Taxonomy.apply _).tupled, Taxonomy.unapply)
}

object Taxonomies
    extends ObjectHeadsQueries[Taxonomy, Taxonomies](new Taxonomies(_))
    with ReturningId[Taxonomy, Taxonomies] {

  val returningLens: Lens[Taxonomy, Int] = lens[Taxonomy].id
}
