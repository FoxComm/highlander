package models.taxonomy

import java.time.Instant

import models.objects._
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.Validation
import utils.db._

object Term {
  val kind = "term"
}

case class Term(id: Int = 0,
                contextId: Int,
                shadowId: Int,
                formId: Int,
                commitId: Int,
                updatedAt: Instant = Instant.now,
                createdAt: Instant = Instant.now,
                archivedAt: Option[Instant] = None)
    extends FoxModel[Term]
    with Validation[Term]
    with ObjectHead[Term] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Term =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class Terms(tag: Tag) extends ObjectHeads[Term](tag, "taxon_terms") {

  def * =
    (id, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Term.apply _).tupled, Term.unapply)
}

object Terms extends ObjectHeadsQueries[Term, Terms](new Terms(_)) with ReturningId[Term, Terms] {

  val returningLens: Lens[Term, Int] = lens[Term].id
}
