package phoenix.models.image

import java.time.Instant

import cats.implicits._
import core.failures._
import objectframework.models._
import phoenix.failures.ArchiveFailures._
import shapeless._
import slick.lifted.Tag
import core.db.ExPostgresDriver.api._
import core.db._
import com.github.tminglei.slickpg._
import core.utils.Validation
import phoenix.utils.JsonFormatters

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

  def mustNotBeArchived: Either[Failures, Album] =
    if (archivedAt.isEmpty) Either.right(this)
    else Either.left(AddImagesToArchivedAlbumFailure(id).single)
}

class Albums(tag: Tag) extends ObjectHeads[Album](tag, "albums") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Album.apply _).tupled, Album.unapply)
}

object Albums extends FoxTableQuery[Album, Albums](new Albums(_)) with ReturningId[Album, Albums] {

  val returningLens: Lens[Album, Int] = lens[Album].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContext(contextId: Int): QuerySeq =
    filter(_.contextId === contextId)

  def filterByFormId(formId: Int): QuerySeq =
    filter(_.formId === formId)

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filterByContext(contextId).filter(_.formId === formId)
}
