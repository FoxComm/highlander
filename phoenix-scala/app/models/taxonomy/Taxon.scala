package models.taxonomy
import java.time.Instant

import models.objects._
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.Validation
import utils.db._

object Taxon {
  val kind = "taxon"
}

case class Taxon(id: Int = 0,
                 hierarchical: Boolean,
                 contextId: Int,
                 formId: Int,
                 shadowId: Int,
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

  def hierarchical = column[Boolean]("hierarchical")

  def * =
    (id, hierarchical, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Taxon.apply _).tupled, Taxon.unapply)
}

object Taxons
    extends ObjectHeadsQueries[Taxon, Taxons](new Taxons(_))
    with ReturningId[Taxon, Taxons] {

  val returningLens: Lens[Taxon, Int] = lens[Taxon].id
}
