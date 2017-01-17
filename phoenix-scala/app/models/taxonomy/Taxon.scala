package models.taxonomy

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.Validation
import utils.db._

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

object Taxons
    extends ObjectHeadsQueries[Taxon, Taxons](new Taxons(_))
    with ReturningId[Taxon, Taxons] {

  val returningLens: Lens[Taxon, Int] = lens[Taxon].id
}
