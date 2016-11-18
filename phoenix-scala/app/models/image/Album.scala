package models.image

import java.time.Instant

import cats.data.Xor
import failures.ArchiveFailures._
import failures._
import models.objects._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.db._
import utils.{JsonFormatters, Validation}

import com.github.tminglei.slickpg._

object Album {
  val kind = "album"
}

case class Album(id: Int = 0,
                 scope: LTree,
                 contextId: Int,
                 shadowId: Int,
                 formId: Int,
                 commitId: Int,
                 updatedAt: Instant = Instant.now,
                 createdAt: Instant = Instant.now,
                 archivedAt: Option[Instant] = None)
    extends FoxModel[Album]
    with Validation[Album]
    with ObjectHead[Album] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Album =
    this.copy(shadowId = shadowId, commitId = commitId)

  def mustNotBeArchived: Failures Xor Album = {
    if (archivedAt.isEmpty) Xor.right(this)
    else Xor.left(AddImagesToArchivedAlbumFailure(id).single)
  }
}

class Albums(tag: Tag) extends ObjectHeads[Album](tag, "albums") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Album.apply _).tupled, Album.unapply)
}

object Albums
    extends ObjectHeadsQueries[Album, Albums](new Albums(_))
    with ReturningId[Album, Albums] {

  val returningLens: Lens[Album, Int] = lens[Album].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filterByContext(contextId).filter(_.formId === formId)
}
