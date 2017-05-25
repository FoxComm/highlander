package phoenix.models.image

import java.time.Instant

import com.github.tminglei.slickpg._
import core.db.ExPostgresDriver.api._
import core.db._
import core.utils.Validation
import objectframework.models._
import phoenix.utils.JsonFormatters
import shapeless._
import slick.lifted.Tag

object Image {
  val kind = "image"
}

case class Image(id: Int = 0,
                 scope: LTree,
                 contextId: Int,
                 shadowId: Int,
                 formId: Int,
                 commitId: Int,
                 updatedAt: Instant = Instant.now,
                 createdAt: Instant = Instant.now,
                 archivedAt: Option[Instant] = None)
    extends FoxModel[Image]
    with Validation[Image]
    with ObjectHead[Image] {

  def withNewShadowAndCommit(shadowId: Int, commitId: Int): Image =
    this.copy(shadowId = shadowId, commitId = commitId)
}

class Images(tag: Tag) extends ObjectHeads[Image](tag, "images") {
  def * =
    (id, scope, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Image.apply _).tupled, Image.unapply)
}

object Images extends FoxTableQuery[Image, Images](new Images(_)) with ReturningId[Image, Images] {
  val returningLens: Lens[Image, Int] = lens[Image].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filter(q ⇒ q.contextId === contextId && q.formId === formId)

  def filterByIds(ids: Seq[Int]): QuerySeq = filter(_.id inSet ids)
}
