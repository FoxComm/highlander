package phoenix.models.taxonomy

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Validation
import objectframework.models._
import shapeless._
import slick.lifted.Tag

object Taxon {
  val kind = "taxon"
}

case class Taxon(id: Int = 0,
                 scope: LTree,
                 contextId: Int,
                 shadowId: Int,
                 formId: Int,
                 commitId: Int,
                 updatedAt: Instant = Instant.now,
                 createdAt: Instant = Instant.now,
                 archivedAt: Option[Instant] = None)
    extends FoxModel[Taxon]
    with Validation[Taxon]
    with ObjectHead[Taxon] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Taxon =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class Taxons(tag: Tag) extends ObjectHeads[Taxon](tag, "taxons") {

  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Taxon.apply _).tupled,
    Taxon.unapply)
}

object Taxons extends ObjectHeadsQueries[Taxon, Taxons](new Taxons(_)) with ReturningId[Taxon, Taxons] {

  val returningLens: Lens[Taxon, Int] = lens[Taxon].id
}
