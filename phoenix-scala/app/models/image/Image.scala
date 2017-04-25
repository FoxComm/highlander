package models.image

import com.github.tminglei.slickpg.LTree
import java.time.Instant
import models.objects._
import shapeless._
import slick.lifted.Tag
import utils.Validation
import utils.db.ExPostgresDriver.api._
import utils.db._

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

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filter(q ⇒ q.contextId === contextId && q.formId === formId)

  def filterByIds(ids: Seq[Int]): QuerySeq = filter(_.id inSet ids)
}
