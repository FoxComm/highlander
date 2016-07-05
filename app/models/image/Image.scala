package models.image

import java.time.Instant

import cats.data.Xor
import cats.data.Xor.{left, right}
import failures.{Failures, GeneralFailure}
import models.objects._
import payloads.ImagePayloads.ImagePayload
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db
import utils.db._
import utils.{Validation, IlluminateAlgorithm, JsonFormatters}

object Image {
  val kind = "image"
}

case class Image(id: Int = 0,
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
    (id, contextId, shadowId, formId, commitId, updatedAt, createdAt, archivedAt) <> ((Image.apply _).tupled, Image.unapply)
}

object Images extends FoxTableQuery[Image, Images](new Images(_)) with ReturningId[Image, Images] {
  val returningLens: Lens[Image, Int] = lens[Image].id

  implicit val formats = JsonFormatters.phoenixFormats

  def filterByContextAndFormId(contextId: Int, formId: Int): QuerySeq =
    filter(q ⇒ q.contextId === contextId && q.formId === formId)
}
